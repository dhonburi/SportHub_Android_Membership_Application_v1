using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.DTOs;
using SportHub.Api.Models;

namespace SportHub.Api.Services;

public class AuthService
{
    private readonly SportHubDbContext _dbContext;

    private readonly IPasswordHasher<User> _passwordHasher;

    public AuthService(
        SportHubDbContext dbContext,
        IPasswordHasher<User> passwordHasher)
    {
        _dbContext = dbContext;
        _passwordHasher = passwordHasher;
    }

    public async Task<LoginResponseDto> LoginAsync(
        LoginRequestDto request)
    {
        string normalizedEmail =
            request.Email
                .Trim()
                .ToLowerInvariant();

        User? user =
            await _dbContext.Users
                .AsNoTracking()
                .Include(userAccount => userAccount.Member)
                .SingleOrDefaultAsync(
                    userAccount =>
                        userAccount.Email == normalizedEmail
                );

        if (user == null ||
            !user.IsActive ||
            user.Member == null)
        {
            return CreateFailedLoginResponse();
        }

        PasswordVerificationResult passwordResult =
            _passwordHasher.VerifyHashedPassword(
                user,
                user.PasswordHash,
                request.Password
            );

        if (passwordResult ==
            PasswordVerificationResult.Failed)
        {
            return CreateFailedLoginResponse();
        }

        return new LoginResponseDto
        {
            Success = true,
            UserId = user.UserId,
            MemberId = user.MemberId,
            MemberNumber = user.Member.MemberNumber,
            Message = "Login successful"
        };
    }

    private static LoginResponseDto
        CreateFailedLoginResponse()
    {
        return new LoginResponseDto
        {
            Success = false,
            UserId = null,
            MemberId = null,
            MemberNumber = null,
            Message = "Invalid email or password"
        };
    }
}