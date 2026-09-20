using Microsoft.AspNetCore.Identity;
using Microsoft.Data.SqlClient;
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
            IsAdmin = user.IsAdmin,
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
            IsAdmin = false,
            Message = "Invalid email or password"
        };
    }

    public async Task<RegisterResponseDto> RegisterAsync(
        RegisterRequestDto request,
        CancellationToken cancellationToken = default)
    {
        if (await _dbContext.Users.AsNoTracking().AnyAsync(
                user => user.Email == request.Email,
                cancellationToken))
        {
            return DuplicateEmailResponse();
        }

        // The unique indexes remain the final guard against
        // concurrent registration requests.
        for (int attempt = 0; attempt < 3; attempt++)
        {
            DateTime createdAt = DateTime.UtcNow;

            var member = new Member
            {
                MemberNumber =
                    "SH-" +
                    Guid.NewGuid()
                        .ToString("N")[..12]
                        .ToUpperInvariant(),

                FirstName = request.FirstName,
                LastName = request.LastName,
                Phone = request.Phone,
                Gender = request.Gender,
                DateOfBirth = request.DateOfBirth?.Date,
                Balance = 0.00m,
                CreatedAt = createdAt
            };

            var user = new User
            {
                Email = request.Email,
                Member = member,
                IsActive = true,
                IsAdmin = false,
                CreatedAt = createdAt
            };

            member.User = user;

            user.PasswordHash =
                _passwordHasher.HashPassword(
                    user,
                    request.Password
                );

            _dbContext.Users.Add(user);

            try
            {
                await _dbContext.SaveChangesAsync(
                    cancellationToken
                );

                return new RegisterResponseDto
                {
                    Success = true,
                    Code = "registered",
                    Message =
                        "Account created successfully. " +
                        "You can now sign in.",

                    MemberId = member.MemberId,
                    MemberNumber = member.MemberNumber
                };
            }
            catch (DbUpdateException exception) when (
                exception.InnerException
                    is SqlException sqlException &&
                sqlException.Errors
                    .Cast<SqlError>()
                    .Any(error =>
                        error.Number is 2601 or 2627))
            {
                _dbContext.ChangeTracker.Clear();

                if (await _dbContext.Users
                        .AsNoTracking()
                        .AnyAsync(
                            account =>
                                account.Email ==
                                request.Email,

                            cancellationToken
                        ))
                {
                    return DuplicateEmailResponse();
                }
            }
        }

        return new RegisterResponseDto
        {
            Code = "temporarily_unavailable",
            Message =
                "Account creation is temporarily unavailable. " +
                "Please try again."
        };
    }

    private static RegisterResponseDto
        DuplicateEmailResponse()
    {
        const string message =
            "An account already uses this email. " +
            "If you already submitted this form, try signing in.";

        return new RegisterResponseDto
        {
            Code = "duplicate_email",
            Message = message,
            Errors = new Dictionary<string, string[]>
            {
                ["email"] = new[] { message }
            }
        };
    }
}