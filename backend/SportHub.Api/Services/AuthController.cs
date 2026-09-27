using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.DTOs;
using SportHub.Api.Services;

namespace SportHub.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly AuthService _authService;
    private readonly StaffTokenService _staffTokenService;
    private readonly ILogger<AuthController> _logger;

    public AuthController(
        AuthService authService,
        StaffTokenService staffTokenService,
        ILogger<AuthController> logger
    )
    {
        _authService = authService;
        _staffTokenService = staffTokenService;
        _logger = logger;
    }

    [HttpPost("login")]
    public async Task<ActionResult<LoginResponseDto>> Login(
        LoginRequestDto request
    )
    {
        LoginResponseDto response =
            await _authService.LoginAsync(request);

        if (response.Success
            && response.IsAdmin
            && response.UserId is int userId)
        {
            var staffToken =
                _staffTokenService.CreateAdminToken(userId);

            response.StaffAccessToken =
                staffToken.Token;

            response.StaffAccessTokenExpiresAtUtc =
                staffToken.ExpiresAtUtc;
        }

        return Ok(response);
    }

    [HttpPost("register")]
    [RequestSizeLimit(16 * 1024)]
    public async Task<ActionResult<RegisterResponseDto>> Register(
        RegisterRequestDto request,
        CancellationToken cancellationToken
    )
    {
        try
        {
            RegisterResponseDto response =
                await _authService.RegisterAsync(
                    request,
                    cancellationToken
                );

            int statusCode = response.Success
                ? StatusCodes.Status201Created
                : response.Code == "duplicate_email"
                    ? StatusCodes.Status409Conflict
                    : StatusCodes.Status503ServiceUnavailable;

            return StatusCode(statusCode, response);
        }
        catch (Exception exception) when (
            exception is DbUpdateException
                or SqlException
                or TimeoutException
        )
        {
            _logger.LogWarning(
                "Registration database operation failed ({FailureType}).",
                exception.GetType().Name
            );

            return StatusCode(
                StatusCodes.Status503ServiceUnavailable,
                new RegisterResponseDto
                {
                    Code = "temporarily_unavailable",
                    Message =
                        "We could not confirm account creation. " +
                        "Please try again, or sign in if you already submitted."
                }
            );
        }
    }
}