using System.Data;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.DTOs;
using SportHub.Api.Models;
using SportHub.Api.Services;

namespace SportHub.Api.Controllers;

[ApiController]
[Route("api/gate-entry")]
public class GateEntryController : ControllerBase
{
    private const string StandardMembershipAccess =
        "STANDARD_MEMBERSHIP";

    private const string SportsPasscardAccess =
        "SPORTS_PASSCARD";

    private const string BalanceAccess =
        "BALANCE";

    private readonly SportHubDbContext _dbContext;
    private readonly QrTokenService _qrTokenService;
    private readonly IConfiguration _configuration;

    public GateEntryController(
        SportHubDbContext dbContext,
        QrTokenService qrTokenService,
        IConfiguration configuration
    )
    {
        _dbContext = dbContext;
        _qrTokenService = qrTokenService;
        _configuration = configuration;
    }

    [HttpPost("process")]
    public async Task<ActionResult<GateEntryResponseDto>>
        ProcessGateEntry(GateEntryRequestDto? request)
    {
        QrTokenValidationResult tokenResult =
            _qrTokenService.ValidateToken(request?.QrToken);

        GateEntryResponseDto? invalidTokenResult =
            CreateInvalidTokenResult(tokenResult);

        if (invalidTokenResult != null)
        {
            return Ok(invalidTokenResult);
        }

        string qrToken = request!.QrToken!;

        string tokenHash = Convert.ToHexString(
            SHA256.HashData(
                Encoding.UTF8.GetBytes(qrToken)
            )
        );

        await using var databaseTransaction =
            await _dbContext.Database.BeginTransactionAsync(
                IsolationLevel.Serializable
            );

        GateEntryProcessing? existingProcessing =
            await FindExistingProcessingAsync(tokenHash);

        if (existingProcessing != null)
        {
            await databaseTransaction.RollbackAsync();
            return Ok(CreateDuplicateResult(existingProcessing));
        }

        if (tokenResult.Status ==
            QrTokenValidationStatus.Expired)
        {
            await databaseTransaction.RollbackAsync();

            return Ok(CreateDeniedResult(
                "EXPIRED_TOKEN",
                "Access Denied: this QR token has expired. Scan the refreshed code."
            ));
        }

        GateEntryResponseDto result =
            tokenResult.Type == QrTokenType.Membership
                ? await ProcessMembershipEntryAsync(
                    tokenResult.Id,
                    tokenHash
                )
                : await ProcessBalanceEntryAsync(
                    tokenResult.Id,
                    tokenHash
                );

        if (!result.IsApproved)
        {
            await databaseTransaction.RollbackAsync();
            return Ok(result);
        }

        try
        {
            await _dbContext.SaveChangesAsync();
            await databaseTransaction.CommitAsync();
            return Ok(result);
        }
        catch (DbUpdateException)
        {
            await databaseTransaction.RollbackAsync();
            _dbContext.ChangeTracker.Clear();

            GateEntryProcessing? concurrentProcessing =
                await FindExistingProcessingAsync(tokenHash);

            if (concurrentProcessing != null)
            {
                return Ok(
                    CreateDuplicateResult(concurrentProcessing)
                );
            }

            return StatusCode(
                StatusCodes.Status503ServiceUnavailable,
                CreateDeniedResult(
                    "GATE_PROCESSING_UNAVAILABLE",
                    "The simulated gate could not safely complete this entry. No confirmed deduction was recorded."
                )
            );
        }
    }

    private async Task<GateEntryResponseDto>
        ProcessMembershipEntryAsync(
            int memberMembershipId,
            string tokenHash
        )
    {
        MemberMembership? membership =
            await _dbContext.MemberMemberships
                .Include(existing => existing.Member)
                .Include(existing =>
                    existing.MembershipPlan
                )
                .SingleOrDefaultAsync(existing =>
                    existing.MemberMembershipId ==
                    memberMembershipId
                );

        if (membership == null)
        {
            return CreateDeniedResult(
                "MEMBERSHIP_NOT_FOUND",
                "Access Denied: the membership does not exist.",
                "MEMBERSHIP"
            );
        }

        bool isSportsPasscard =
            string.Equals(
                membership.MembershipPlan.PlanName,
                "Sports Passcard",
                StringComparison.OrdinalIgnoreCase
            );

        string accessMethod = isSportsPasscard
            ? SportsPasscardAccess
            : StandardMembershipAccess;

        DateTime today = GetNewZealandToday();

        bool isExpired =
            string.Equals(
                membership.Status,
                "Expired",
                StringComparison.OrdinalIgnoreCase
            )
            || (
                membership.ExpiryDate.HasValue
                && membership.ExpiryDate.Value.Date < today
            );

        if (isExpired)
        {
            return CreateDeniedResult(
                "EXPIRED_MEMBERSHIP",
                "Access Denied: this membership has expired.",
                accessMethod,
                membership.Member.MemberNumber,
                membership.MembershipPlan.PlanName,
                membership.Status,
                membership.RemainingEntries,
                membership.Member.Balance
            );
        }

        bool isActive =
            string.Equals(
                membership.Status,
                "Active",
                StringComparison.OrdinalIgnoreCase
            );

        if (!isActive)
        {
            return CreateDeniedResult(
                "INACTIVE_MEMBERSHIP",
                "Access Denied: this membership is not active.",
                accessMethod,
                membership.Member.MemberNumber,
                membership.MembershipPlan.PlanName,
                membership.Status,
                membership.RemainingEntries,
                membership.Member.Balance
            );
        }

        int entriesDeducted = 0;

        if (isSportsPasscard)
        {
            if (!membership.RemainingEntries.HasValue)
            {
                return CreateDeniedResult(
                    "PASSCARD_CONFIGURATION_INVALID",
                    "Access Denied: the Sports Passcard entry balance is unavailable.",
                    accessMethod,
                    membership.Member.MemberNumber,
                    membership.MembershipPlan.PlanName,
                    membership.Status,
                    null,
                    membership.Member.Balance
                );
            }

            if (membership.RemainingEntries.Value <= 0)
            {
                return CreateDeniedResult(
                    "NO_REMAINING_ENTRIES",
                    "Access Denied: this Sports Passcard has no remaining entries.",
                    accessMethod,
                    membership.Member.MemberNumber,
                    membership.MembershipPlan.PlanName,
                    membership.Status,
                    membership.RemainingEntries,
                    membership.Member.Balance
                );
            }

            membership.RemainingEntries -= 1;
            membership.UpdatedAt = DateTime.UtcNow;
            entriesDeducted = 1;
        }

        GateEntryProcessing processing =
            CreateProcessing(
                tokenHash: tokenHash,
                member: membership.Member,
                membership: membership,
                accessMethod: accessMethod,
                amountCharged: 0m,
                entriesDeducted: entriesDeducted,
                remainingEntriesAfter:
                    membership.RemainingEntries
            );

        _dbContext.GateEntryProcessings.Add(processing);

        string message = isSportsPasscard
            ? "Access Approved: one Sports Passcard entry was used."
            : "Access Approved: entry was granted through the active membership.";

        return CreateApprovedResult(
            processing,
            message,
            membership.Member.MemberNumber,
            membership.MembershipPlan.PlanName,
            membership.Status
        );
    }

    private async Task<GateEntryResponseDto>
        ProcessBalanceEntryAsync(
            int memberId,
            string tokenHash
        )
    {
        Member? member =
            await _dbContext.Members
                .SingleOrDefaultAsync(existing =>
                    existing.MemberId == memberId
                );

        if (member == null)
        {
            return CreateDeniedResult(
                "MEMBER_NOT_FOUND",
                "Access Denied: the member does not exist.",
                BalanceAccess
            );
        }

        decimal casualEntryCharge =
            _configuration.GetValue(
                "Gate:CasualEntryCharge",
                _configuration.GetValue(
                    "Qr:MinimumBalanceForBalanceQrCode",
                    5m
                )
            );

        if (casualEntryCharge <= 0)
        {
            return CreateDeniedResult(
                "GATE_CONFIGURATION_ERROR",
                "Access Denied: balance-based entry is temporarily unavailable.",
                BalanceAccess,
                member.MemberNumber,
                balance: member.Balance
            );
        }

        if (member.Balance < casualEntryCharge)
        {
            return CreateDeniedResult(
                "INSUFFICIENT_BALANCE",
                $"Access Denied: a balance of at least {casualEntryCharge:0.00} NZD is required.",
                BalanceAccess,
                member.MemberNumber,
                balance: member.Balance
            );
        }

        member.Balance -= casualEntryCharge;

        GateEntryProcessing processing =
            CreateProcessing(
                tokenHash: tokenHash,
                member: member,
                membership: null,
                accessMethod: BalanceAccess,
                amountCharged: casualEntryCharge,
                entriesDeducted: 0,
                remainingEntriesAfter: null
            );

        _dbContext.GateEntryProcessings.Add(processing);

        _dbContext.Transactions.Add(
            new MemberTransaction
            {
                MemberId = member.MemberId,
                Member = member,
                OperationId =
                    processing.ProcessingId.ToString("D"),
                TransactionType =
                    MemberTransaction.GateEntryChargeType,
                Description = "Simulated Gate Entry",
                Amount = -casualEntryCharge,
                BalanceAfter = member.Balance,
                OccurredAtUtc =
                    processing.ProcessedAtUtc
            }
        );

        return CreateApprovedResult(
            processing,
            $"Access Approved: {casualEntryCharge:0.00} NZD was charged from the member balance.",
            member.MemberNumber
        );
    }

    private static GateEntryProcessing CreateProcessing(
        string tokenHash,
        Member member,
        MemberMembership? membership,
        string accessMethod,
        decimal amountCharged,
        int entriesDeducted,
        int? remainingEntriesAfter
    )
    {
        return new GateEntryProcessing
        {
            ProcessingId = Guid.NewGuid(),
            TokenHash = tokenHash,
            MemberId = member.MemberId,
            Member = member,
            MemberMembershipId =
                membership?.MemberMembershipId,
            MemberMembership = membership,
            AccessMethod = accessMethod,
            AmountCharged = amountCharged,
            EntriesDeducted = entriesDeducted,
            BalanceAfter = member.Balance,
            RemainingEntriesAfter =
                remainingEntriesAfter,
            Status = "Completed",
            ProcessedAtUtc = DateTime.UtcNow
        };
    }

    private Task<GateEntryProcessing?>
        FindExistingProcessingAsync(string tokenHash)
    {
        return _dbContext.GateEntryProcessings
            .AsNoTracking()
            .Include(entry => entry.Member)
            .Include(entry => entry.MemberMembership)
            .ThenInclude(membership =>
                membership!.MembershipPlan
            )
            .SingleOrDefaultAsync(entry =>
                entry.TokenHash == tokenHash
            );
    }

    private static GateEntryResponseDto?
        CreateInvalidTokenResult(
            QrTokenValidationResult tokenResult
        )
    {
        return tokenResult.Status switch
        {
            QrTokenValidationStatus.Missing =>
                CreateDeniedResult(
                    "MISSING_TOKEN",
                    "Access Denied: a QR token is required."
                ),

            QrTokenValidationStatus.Malformed =>
                CreateDeniedResult(
                    "MALFORMED_TOKEN",
                    "Access Denied: this is not a supported SportHub QR code."
                ),

            QrTokenValidationStatus.InvalidSignature =>
                CreateDeniedResult(
                    "INVALID_SIGNATURE",
                    "Access Denied: the QR token signature is invalid."
                ),

            _ => null
        };
    }

    private static GateEntryResponseDto
        CreateApprovedResult(
            GateEntryProcessing processing,
            string message,
            string memberNumber,
            string? planName = null,
            string? membershipStatus = null
        )
    {
        return new GateEntryResponseDto
        {
            IsApproved = true,
            IsDuplicate = false,
            Decision = "APPROVED",
            ResultCode = "ENTRY_PROCESSED",
            Message = message,
            ProcessingId =
                processing.ProcessingId.ToString("D"),
            AccessType = processing.AccessMethod,
            MemberNumber = memberNumber,
            PlanName = planName,
            MembershipStatus = membershipStatus,
            RemainingEntries =
                processing.RemainingEntriesAfter,
            AmountCharged = processing.AmountCharged,
            Balance = processing.BalanceAfter,
            ProcessedAtUtc =
                FormatUtc(processing.ProcessedAtUtc)
        };
    }

    private static GateEntryResponseDto
        CreateDuplicateResult(
            GateEntryProcessing processing
        )
    {
        return new GateEntryResponseDto
        {
            IsApproved = false,
            IsDuplicate = true,
            Decision = "DUPLICATE",
            ResultCode = "ALREADY_PROCESSED",
            Message = "This QR token has already been processed. No additional deduction was made.",
            ProcessingId =
                processing.ProcessingId.ToString("D"),
            AccessType = processing.AccessMethod,
            MemberNumber =
                processing.Member.MemberNumber,
            PlanName = processing.MemberMembership?
                .MembershipPlan.PlanName,
            MembershipStatus =
                processing.MemberMembership?.Status,
            RemainingEntries =
                processing.RemainingEntriesAfter,
            AmountCharged = processing.AmountCharged,
            Balance = processing.BalanceAfter,
            ProcessedAtUtc =
                FormatUtc(processing.ProcessedAtUtc)
        };
    }

    private static GateEntryResponseDto
        CreateDeniedResult(
            string resultCode,
            string message,
            string? accessType = null,
            string? memberNumber = null,
            string? planName = null,
            string? membershipStatus = null,
            int? remainingEntries = null,
            decimal? balance = null
        )
    {
        return new GateEntryResponseDto
        {
            IsApproved = false,
            IsDuplicate = false,
            Decision = "DENIED",
            ResultCode = resultCode,
            Message = message,
            AccessType = accessType,
            MemberNumber = memberNumber,
            PlanName = planName,
            MembershipStatus = membershipStatus,
            RemainingEntries = remainingEntries,
            Balance = balance
        };
    }

    private static DateTime GetNewZealandToday()
    {
        TimeZoneInfo newZealandTimeZone =
            TimeZoneInfo.FindSystemTimeZoneById(
                "Pacific/Auckland"
            );

        return TimeZoneInfo.ConvertTimeFromUtc(
            DateTime.UtcNow,
            newZealandTimeZone
        ).Date;
    }

    private static string FormatUtc(DateTime value)
    {
        return value.ToString(
            "yyyy-MM-ddTHH:mm:ssZ",
            System.Globalization.CultureInfo.InvariantCulture
        );
    }
}