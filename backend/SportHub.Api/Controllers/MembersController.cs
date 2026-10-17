using System.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using SportHub.Api.Data;
using SportHub.Api.DTOs;
using SportHub.Api.Models;
using SportHub.Api.Services;

namespace SportHub.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class MembersController : ControllerBase
{
    private const decimal MaximumMemberBalance =
        99_999_999.99m;

    private readonly SportHubDbContext _dbContext;

    private readonly QrTokenService _qrTokenService;

    private readonly IConfiguration _configuration;

    public MembersController(
        SportHubDbContext dbContext,
        QrTokenService qrTokenService,
        IConfiguration configuration
    )
    {
        _dbContext = dbContext;
        _qrTokenService = qrTokenService;
        _configuration = configuration;
    }

    [HttpGet("{memberId:int}")]
    public async Task<ActionResult<MemberProfileResponseDto>>
        GetMemberProfile(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        MemberProfileResponseDto? memberProfile =
            await _dbContext.Members
                .AsNoTracking()
                .Where(member =>
                    member.MemberId == memberId
                )
                .Select(member =>
                    new MemberProfileResponseDto
                    {
                        MemberId = member.MemberId,
                        MemberNumber = member.MemberNumber,
                        FirstName = member.FirstName,
                        LastName = member.LastName,
                        Email = member.User == null
                            ? null
                            : member.User.Email,
                        Phone = member.Phone,
                        Gender = member.Gender,
                        Balance = member.Balance
                    }
                )
                .SingleOrDefaultAsync();

        if (memberProfile == null)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        return Ok(memberProfile);
    }

    [HttpPut("{memberId:int}")]
    public async Task<ActionResult<MemberProfileResponseDto>>
        UpdateMemberProfile(
            int memberId,
            UpdateMemberProfileRequestDto request
        )
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        string firstName = request.FirstName.Trim();
        string lastName = request.LastName.Trim();

        string normalizedEmail =
            request.Email
                .Trim()
                .ToLowerInvariant();

        if (string.IsNullOrWhiteSpace(firstName))
        {
            return BadRequest(
                "First name is required."
            );
        }

        if (string.IsNullOrWhiteSpace(lastName))
        {
            return BadRequest(
                "Last name is required."
            );
        }

        Member? member =
            await _dbContext.Members
                .Include(existingMember =>
                    existingMember.User
                )
                .SingleOrDefaultAsync(existingMember =>
                    existingMember.MemberId == memberId
                );

        if (member == null)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        if (member.User == null)
        {
            return Conflict(
                "The member does not have a linked login account."
            );
        }

        bool emailAlreadyInUse =
            await _dbContext.Users
                .AsNoTracking()
                .AnyAsync(user =>
                    user.UserId != member.User.UserId
                    && user.Email == normalizedEmail
                );

        if (emailAlreadyInUse)
        {
            return Conflict(
                "That email address is already in use."
            );
        }

        member.FirstName = firstName;
        member.LastName = lastName;

        member.Phone =
            string.IsNullOrWhiteSpace(request.Phone)
                ? null
                : request.Phone.Trim();

        member.Gender =
            string.IsNullOrWhiteSpace(request.Gender)
                ? null
                : request.Gender.Trim();

        member.User.Email = normalizedEmail;

        await _dbContext.SaveChangesAsync();

        var updatedProfile =
            new MemberProfileResponseDto
            {
                MemberId = member.MemberId,
                MemberNumber = member.MemberNumber,
                FirstName = member.FirstName,
                LastName = member.LastName,
                Email = member.User.Email,
                Phone = member.Phone,
                Gender = member.Gender,
                Balance = member.Balance
            };

        return Ok(updatedProfile);
    }

    [HttpPost("{memberId:int}/balance/top-up")]
    public async Task<ActionResult<TopUpBalanceResponseDto>>
        TopUpMemberBalance(
            int memberId,
            TopUpBalanceRequestDto request
        )
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        if (request.Amount <= 0)
        {
            return BadRequest(
                "The top-up amount must be greater than zero."
            );
        }

        if (!Guid.TryParse(
                request.OperationId,
                out Guid operationId
            ))
        {
            return BadRequest(
                "A valid operation ID is required."
            );
        }

        decimal roundedAmount =
            decimal.Round(
                request.Amount,
                2,
                MidpointRounding.AwayFromZero
            );

        if (roundedAmount != request.Amount)
        {
            return BadRequest(
                "The top-up amount cannot contain more than two decimal places."
            );
        }

        string normalizedOperationId =
            operationId.ToString("D");

        await using var databaseTransaction =
            await _dbContext.Database.BeginTransactionAsync(
                IsolationLevel.Serializable
            );

        MemberTransaction? existingTransaction =
            await _dbContext.Transactions
                .AsNoTracking()
                .SingleOrDefaultAsync(transaction =>
                    transaction.MemberId == memberId
                    && transaction.OperationId ==
                    normalizedOperationId
                );

        if (existingTransaction != null)
        {
            bool matchesRequest =
                existingTransaction.TransactionType ==
                    MemberTransaction.BalanceTopUpType
                && existingTransaction.Amount == roundedAmount;

            if (!matchesRequest)
            {
                return Conflict(
                    "The operation ID has already been used for another request."
                );
            }

            return Ok(
                new TopUpBalanceResponseDto
                {
                    MemberId = memberId,
                    AmountAdded = existingTransaction.Amount,
                    Balance = existingTransaction.BalanceAfter,
                    Currency = "NZD",
                    Message = "Mock balance was already added successfully."
                }
            );
        }

        Member? member =
            await _dbContext.Members
                .SingleOrDefaultAsync(existingMember =>
                    existingMember.MemberId == memberId
                );

        if (member == null)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        if (member.Balance >
            MaximumMemberBalance - roundedAmount)
        {
            return BadRequest(
                "The top-up would exceed the maximum permitted balance."
            );
        }

        member.Balance += roundedAmount;

        _dbContext.Transactions.Add(
            new MemberTransaction
            {
                MemberId = member.MemberId,
                OperationId = normalizedOperationId,
                TransactionType =
                    MemberTransaction.BalanceTopUpType,
                Description = "Balance Top Up",
                Amount = roundedAmount,
                BalanceAfter = member.Balance,
                OccurredAtUtc = DateTime.UtcNow
            }
        );

        await _dbContext.SaveChangesAsync();
        await databaseTransaction.CommitAsync();

        var response =
            new TopUpBalanceResponseDto
            {
                MemberId = member.MemberId,
                AmountAdded = roundedAmount,
                Balance = member.Balance,
                Currency = "NZD",
                Message = "Mock balance added successfully."
            };

        return Ok(response);
    }

    [HttpGet("{memberId:int}/transactions")]
    public async Task<ActionResult<List<MemberTransactionResponseDto>>>
        GetMemberTransactions(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        bool memberExists =
            await _dbContext.Members
                .AsNoTracking()
                .AnyAsync(member =>
                    member.MemberId == memberId
                );

        if (!memberExists)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        List<MemberTransactionResponseDto> transactions =
            await _dbContext.Transactions
                .AsNoTracking()
                .Where(transaction =>
                    transaction.MemberId == memberId
                )
                .OrderByDescending(transaction =>
                    transaction.OccurredAtUtc
                )
                .ThenByDescending(transaction =>
                    transaction.TransactionId
                )
                .Select(transaction =>
                    new MemberTransactionResponseDto
                    {
                        TransactionId = transaction.TransactionId,
                        MemberId = transaction.MemberId,
                        TransactionType =
                            transaction.TransactionType,
                        Description = transaction.Description,
                        Amount = transaction.Amount,
                        BalanceAfter = transaction.BalanceAfter,
                        Currency = "NZD",
                        OccurredAtUtc =
                            FormatUtc(transaction.OccurredAtUtc)
                    }
                )
                .ToListAsync();

        return Ok(transactions);
    }

    [HttpGet("{memberId:int}/balance/qr-code")]
    public async Task<ActionResult<BalanceQrCodeResponseDto>>
        GetBalanceQrCode(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        Member? member =
            await _dbContext.Members
                .AsNoTracking()
                .SingleOrDefaultAsync(existingMember =>
                    existingMember.MemberId == memberId
                );

        if (member == null)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        decimal minimumBalance =
            _configuration.GetValue(
                "Qr:MinimumBalanceForBalanceQrCode",
                0m
            );

        if (member.Balance <= minimumBalance)
        {
            return Conflict(
                "Insufficient balance to generate an entry QR code."
            );
        }

        QrTokenResult tokenResult =
            _qrTokenService.IssueToken(
                QrTokenType.Member,
                member.MemberId
            );

        var response =
            new BalanceQrCodeResponseDto
            {
                MemberId = member.MemberId,
                Balance = member.Balance,
                Currency = "NZD",
                QrToken = tokenResult.Token,

                IssuedAtUtc =
                    FormatUtc(tokenResult.IssuedAtUtc),

                ExpiresAtUtc =
                    FormatUtc(tokenResult.ExpiresAtUtc),

                ValiditySeconds =
                    tokenResult.ValiditySeconds
            };

        return Ok(response);
    }

    [HttpGet("{memberId:int}/membership-plans")]
    public async Task<ActionResult<List<MembershipPlanResponseDto>>>
        GetMembershipPlans(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        bool memberExists =
            await _dbContext.Members
                .AsNoTracking()
                .AnyAsync(member =>
                    member.MemberId == memberId
                );

        if (!memberExists)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        DateTime today = GetNewZealandToday();

        List<MembershipPlanResponseDto> plans =
            await _dbContext.MembershipPlans
                .AsNoTracking()
                .OrderBy(plan => plan.Price)
                .ThenBy(plan => plan.PlanName)
                .Select(plan =>
                    new MembershipPlanResponseDto
                    {
                        MembershipPlanId =
                            plan.MembershipPlanId,

                        PlanName = plan.PlanName,
                        Price = plan.Price,
                        Description = plan.Description,

                        IsAlreadyActive =
                            plan.MemberMemberships.Any(
                                membership =>
                                    membership.MemberId == memberId
                                    && membership.Status == "Active"
                                    && (
                                        membership.ExpiryDate == null
                                        || membership.ExpiryDate >= today
                                    )
                            )
                    }
                )
                .ToListAsync();

        return Ok(plans);
    }

    [HttpPost("{memberId:int}/memberships/purchase")]
    public async Task<ActionResult<PurchaseMembershipResponseDto>>
        PurchaseMembership(
            int memberId,
            PurchaseMembershipRequestDto request
        )
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        if (request.MembershipPlanId <= 0)
        {
            return BadRequest(
                "A valid membership plan ID is required."
            );
        }

        if (!Guid.TryParse(
                request.OperationId,
                out Guid operationId
            ))
        {
            return BadRequest(
                "A valid operation ID is required."
            );
        }

        string normalizedOperationId =
            operationId.ToString("D");

        await using var transaction =
            await _dbContext.Database.BeginTransactionAsync(
                IsolationLevel.Serializable
            );

        MemberTransaction? existingTransaction =
            await _dbContext.Transactions
                .AsNoTracking()
                .Include(existing =>
                    existing.MemberMembership
                )
                .ThenInclude(membership =>
                    membership!.MembershipPlan
                )
                .SingleOrDefaultAsync(existing =>
                    existing.MemberId == memberId
                    && existing.OperationId ==
                    normalizedOperationId
                );

        if (existingTransaction != null)
        {
            MemberMembership? savedMembership =
                existingTransaction.MemberMembership;

            bool matchesRequest =
                existingTransaction.TransactionType ==
                    MemberTransaction.MembershipPurchaseType
                && savedMembership != null
                && savedMembership.MembershipPlanId ==
                    request.MembershipPlanId;

            if (!matchesRequest)
            {
                return Conflict(
                    "The operation ID has already been used for another request."
                );
            }

            return Ok(
                CreatePurchaseResponse(
                    savedMembership!,
                    savedMembership!.MembershipPlan,
                    existingTransaction.BalanceAfter,
                    "Membership purchase was already completed."
                )
            );
        }

        Member? member =
            await _dbContext.Members
                .SingleOrDefaultAsync(existingMember =>
                    existingMember.MemberId == memberId
                );

        if (member == null)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        MembershipPlan? plan =
            await _dbContext.MembershipPlans
                .SingleOrDefaultAsync(existingPlan =>
                    existingPlan.MembershipPlanId ==
                    request.MembershipPlanId
                );

        if (plan == null)
        {
            return NotFound(
                "Membership plan was not found."
            );
        }

        DateTime today = GetNewZealandToday();

        bool alreadyActive =
            await _dbContext.MemberMemberships
                .AnyAsync(membership =>
                    membership.MemberId == memberId
                    && membership.MembershipPlanId ==
                    plan.MembershipPlanId
                    && membership.Status == "Active"
                    && (
                        membership.ExpiryDate == null
                        || membership.ExpiryDate >= today
                    )
                );

        if (alreadyActive)
        {
            return Conflict(
                "This membership plan is already active for the member."
            );
        }

        if (member.Balance < plan.Price)
        {
            return BadRequest(
                "The member does not have enough balance to purchase this plan."
            );
        }

        bool isSportsPasscard =
            string.Equals(
                plan.PlanName,
                "Sports Passcard",
                StringComparison.OrdinalIgnoreCase
            );

        var membership =
            new MemberMembership
            {
                MemberId = member.MemberId,
                MembershipPlanId = plan.MembershipPlanId,
                Status = "Active",
                StartDate = today,
                ExpiryDate = isSportsPasscard
                    ? null
                    : today.AddYears(1).AddDays(-1),
                RemainingEntries = isSportsPasscard
                    ? 10
                    : null,
                CreatedAt = DateTime.UtcNow
            };

        member.Balance -= plan.Price;

        _dbContext.MemberMemberships.Add(membership);

        _dbContext.Transactions.Add(
            new MemberTransaction
            {
                MemberId = member.MemberId,
                MemberMembership = membership,
                OperationId = normalizedOperationId,
                TransactionType =
                    MemberTransaction.MembershipPurchaseType,
                Description =
                    $"{plan.PlanName} Purchase",
                Amount = -plan.Price,
                BalanceAfter = member.Balance,
                OccurredAtUtc = DateTime.UtcNow
            }
        );

        await _dbContext.SaveChangesAsync();
        await transaction.CommitAsync();

        PurchaseMembershipResponseDto response =
            CreatePurchaseResponse(
                membership,
                plan,
                member.Balance,
                "Membership purchased successfully."
            );

        return Ok(response);
    }

    [HttpGet("{memberId:int}/membership")]
    public async Task<ActionResult<MemberMembershipResponseDto>>
        GetMemberMembership(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        MemberMembershipResponseDto? membership =
            await _dbContext.MemberMemberships
                .AsNoTracking()
                .Where(memberMembership =>
                    memberMembership.MemberId == memberId
                )
                .OrderByDescending(memberMembership =>
                    memberMembership.Status == "Active"
                )
                .ThenByDescending(memberMembership =>
                    memberMembership.StartDate
                )
                .ThenByDescending(memberMembership =>
                    memberMembership.MemberMembershipId
                )
                .Select(memberMembership =>
                    new MemberMembershipResponseDto
                    {
                        MemberMembershipId =
                            memberMembership.MemberMembershipId,

                        MemberNumber =
                            memberMembership.Member.MemberNumber,

                        PlanName =
                            memberMembership.MembershipPlan.PlanName,

                        Price =
                            memberMembership.MembershipPlan.Price,

                        Description =
                            memberMembership.MembershipPlan.Description,

                        Status =
                            memberMembership.Status,

                        StartDate =
                            memberMembership.StartDate,

                        ExpiryDate =
                            memberMembership.ExpiryDate,

                        RemainingEntries =
                            memberMembership.RemainingEntries
                    }
                )
                .FirstOrDefaultAsync();

        if (membership == null)
        {
            return NotFound(
                "No membership was found for this member."
            );
        }

        return Ok(membership);
    }

    [HttpGet("{memberId:int}/memberships")]
    public async Task<ActionResult<List<MemberMembershipResponseDto>>>
        GetMemberMemberships(int memberId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        bool memberExists =
            await _dbContext.Members
                .AsNoTracking()
                .AnyAsync(member =>
                    member.MemberId == memberId
                );

        if (!memberExists)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        List<MemberMembershipResponseDto> memberships =
            await _dbContext.MemberMemberships
                .AsNoTracking()
                .Where(memberMembership =>
                    memberMembership.MemberId == memberId
                )
                .OrderByDescending(memberMembership =>
                    memberMembership.Status == "Active"
                )
                .ThenByDescending(memberMembership =>
                    memberMembership.StartDate
                )
                .ThenByDescending(memberMembership =>
                    memberMembership.MemberMembershipId
                )
                .Select(memberMembership =>
                    new MemberMembershipResponseDto
                    {
                        MemberMembershipId =
                            memberMembership.MemberMembershipId,

                        MemberNumber =
                            memberMembership.Member.MemberNumber,

                        PlanName =
                            memberMembership.MembershipPlan.PlanName,

                        Price =
                            memberMembership.MembershipPlan.Price,

                        Description =
                            memberMembership.MembershipPlan.Description,

                        Status =
                            memberMembership.Status,

                        StartDate =
                            memberMembership.StartDate,

                        ExpiryDate =
                            memberMembership.ExpiryDate,

                        RemainingEntries =
                            memberMembership.RemainingEntries
                    }
                )
                .ToListAsync();

        return Ok(memberships);
    }

    [HttpGet("{memberId:int}/memberships/{memberMembershipId:int}/qr-code")]
    public async Task<ActionResult<MembershipQrCodeResponseDto>>
        GetMembershipQrCode(int memberId, int memberMembershipId)
    {
        if (memberId <= 0)
        {
            return BadRequest(
                "A valid member ID is required."
            );
        }

        if (memberMembershipId <= 0)
        {
            return BadRequest(
                "A valid membership ID is required."
            );
        }

        bool memberExists =
            await _dbContext.Members
                .AsNoTracking()
                .AnyAsync(member =>
                    member.MemberId == memberId
                );

        if (!memberExists)
        {
            return NotFound(
                "Member profile was not found."
            );
        }

        MemberMembership? membership =
            await _dbContext.MemberMemberships
                .Include(memberMembership =>
                    memberMembership.MembershipPlan
                )
                .SingleOrDefaultAsync(memberMembership =>
                    memberMembership.MemberMembershipId ==
                    memberMembershipId
                    && memberMembership.MemberId == memberId
                );

        if (membership == null)
        {
            return NotFound(
                "Membership was not found for this member."
            );
        }

        DateTime today = GetNewZealandToday();

        bool isExpired =
            string.Equals(
                membership.Status,
                "Expired",
                StringComparison.OrdinalIgnoreCase
            )
            || (
                membership.ExpiryDate.HasValue
                && membership.ExpiryDate.Value < today
            );

        if (isExpired)
        {
            return Conflict(
                "Membership has expired."
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
            return Conflict(
                "Membership is not active."
            );
        }

        QrTokenResult tokenResult =
            _qrTokenService.IssueToken(
                QrTokenType.Membership,
                membership.MemberMembershipId
            );

        var response =
            new MembershipQrCodeResponseDto
            {
                MemberMembershipId =
                    membership.MemberMembershipId,

                PlanName =
                    membership.MembershipPlan.PlanName,

                Status = membership.Status,

                QrToken = tokenResult.Token,

                IssuedAtUtc =
                    FormatUtc(tokenResult.IssuedAtUtc),

                ExpiresAtUtc =
                    FormatUtc(tokenResult.ExpiresAtUtc),

                ValiditySeconds =
                    tokenResult.ValiditySeconds
            };

        return Ok(response);
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

    private static PurchaseMembershipResponseDto
        CreatePurchaseResponse(
            MemberMembership membership,
            MembershipPlan plan,
            decimal balance,
            string message
        )
    {
        return new PurchaseMembershipResponseDto
        {
            MemberMembershipId =
                membership.MemberMembershipId,
            MemberId = membership.MemberId,
            MembershipPlanId = plan.MembershipPlanId,
            PlanName = plan.PlanName,
            PricePaid = plan.Price,
            Balance = balance,
            Currency = "NZD",
            Status = membership.Status,
            StartDate = membership.StartDate,
            ExpiryDate = membership.ExpiryDate,
            RemainingEntries = membership.RemainingEntries,
            Message = message
        };
    }

    private static string FormatUtc(DateTime value)
    {
        return value.ToString(
            "yyyy-MM-ddTHH:mm:ssZ",
            System.Globalization.CultureInfo.InvariantCulture
        );
    }
}