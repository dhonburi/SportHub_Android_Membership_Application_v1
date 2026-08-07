using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.DTOs;
using SportHub.Api.Models;

namespace SportHub.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class MembersController : ControllerBase
{
    private readonly SportHubDbContext _dbContext;

    public MembersController(SportHubDbContext dbContext)
    {
        _dbContext = dbContext;
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
                        Gender = member.Gender
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
                Gender = member.Gender
            };

        return Ok(updatedProfile);
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
}