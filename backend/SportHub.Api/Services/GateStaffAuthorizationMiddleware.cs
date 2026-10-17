using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;

namespace SportHub.Api.Services;

public sealed class GateStaffAuthorizationMiddleware
{
    private static readonly PathString GatePath =
        new("/api/gate-entry");

    private readonly RequestDelegate _next;

    public GateStaffAuthorizationMiddleware(
        RequestDelegate next
    )
    {
        _next = next;
    }

    public async Task InvokeAsync(
        HttpContext context,
        SportHubDbContext dbContext
    )
    {
        if (!context.Request.Path.StartsWithSegments(
                GatePath,
                StringComparison.OrdinalIgnoreCase
            ))
        {
            await _next(context);
            return;
        }

        if (context.User.Identity?.IsAuthenticated != true)
        {
            await context.ChallengeAsync(
                JwtBearerDefaults.AuthenticationScheme
            );
            return;
        }

        if (!context.User.IsInRole("Admin"))
        {
            await context.ForbidAsync(
                JwtBearerDefaults.AuthenticationScheme
            );
            return;
        }

        string? userIdValue =
            context.User.FindFirstValue(
                ClaimTypes.NameIdentifier
            );

        if (!int.TryParse(
                userIdValue,
                out int userId
            ))
        {
            await context.ForbidAsync(
                JwtBearerDefaults.AuthenticationScheme
            );
            return;
        }

        bool isCurrentAdmin =
            await dbContext.Users
                .AsNoTracking()
                .AnyAsync(
                    user =>
                        user.UserId == userId
                        && user.IsActive
                        && user.IsAdmin,
                    context.RequestAborted
                );

        if (!isCurrentAdmin)
        {
            await context.ForbidAsync(
                JwtBearerDefaults.AuthenticationScheme
            );
            return;
        }

        await _next(context);
    }
}