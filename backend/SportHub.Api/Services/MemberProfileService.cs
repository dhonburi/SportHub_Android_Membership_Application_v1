using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.DTOs;

namespace SportHub.Api.Services;

public class MemberProfileService
{
    private readonly SportHubDbContext _dbContext;

    public MemberProfileService(SportHubDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<MemberProfileResponseDto?> GetByMemberIdAsync(
        int memberId,
        CancellationToken cancellationToken = default)
    {
        return await _dbContext.Members
            .AsNoTracking()
            .Where(member => member.MemberId == memberId)
            .Select(member => new MemberProfileResponseDto
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
            })
            .SingleOrDefaultAsync(cancellationToken);
    }
}