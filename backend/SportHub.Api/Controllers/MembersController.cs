using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.DTOs;

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