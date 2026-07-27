using Microsoft.AspNetCore.Mvc;
using SportHub.Api.DTOs;
using SportHub.Api.Services;

namespace SportHub.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly AuthService _authService;

    public AuthController(AuthService authService)
    {
        _authService = authService;
    }

    [HttpPost("login")]
    public async Task<ActionResult<LoginResponseDto>> Login(
        LoginRequestDto request)
    {
        LoginResponseDto response =
            await _authService.LoginAsync(request);

        return Ok(response);
    }
}