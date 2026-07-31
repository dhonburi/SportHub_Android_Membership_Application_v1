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
                        LastName = member.LastName
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
}