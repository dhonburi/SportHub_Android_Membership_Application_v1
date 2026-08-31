using System.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
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

    public MembersController(
        SportHubDbContext dbContext,
        QrTokenService qrTokenService
    )
    {
        _dbContext = dbContext;
        _qrTokenService = qrTokenService;
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

        await _dbContext.SaveChangesAsync();

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

        await using var transaction =
            await _dbContext.Database.BeginTransactionAsync(
                IsolationLevel.Serializable
            );

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

        await _dbContext.SaveChangesAsync();
        await transaction.CommitAsync();

        var response =
            new PurchaseMembershipResponseDto
            {
                MemberMembershipId =
                    membership.MemberMembershipId,

                MemberId = member.MemberId,
                MembershipPlanId = plan.MembershipPlanId,
                PlanName = plan.PlanName,
                PricePaid = plan.Price,
                Balance = member.Balance,
                Currency = "NZD",
                Status = membership.Status,
                StartDate = membership.StartDate,
                ExpiryDate = membership.ExpiryDate,
                RemainingEntries =
                    membership.RemainingEntries,

                Message =
                    "Membership purchased successfully."
            };

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

    private static string FormatUtc(DateTime value)
    {
        return value.ToString(
            "yyyy-MM-ddTHH:mm:ssZ",
            System.Globalization.CultureInfo.InvariantCulture
        );
    }
}