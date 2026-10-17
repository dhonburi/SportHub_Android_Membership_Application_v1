using System.Globalization;

namespace SportHub.Api.Services;

public sealed class MemberSessionAuthorizationMiddleware
{
    private readonly RequestDelegate _next;
    public MemberSessionAuthorizationMiddleware(RequestDelegate next) => _next = next;
    public async Task InvokeAsync(HttpContext context, MemberLoginSessionService sessions)
    {
        var path = context.Request.Path;
        var normalizedPath = path.Value?.TrimEnd('/');
        bool memberRoute = path.StartsWithSegments("/api/members", StringComparison.OrdinalIgnoreCase);
        bool sessionRoute = string.Equals(normalizedPath, "/api/auth/session", StringComparison.OrdinalIgnoreCase)
            || string.Equals(normalizedPath, "/api/auth/logout", StringComparison.OrdinalIgnoreCase);
        if (!memberRoute && !sessionRoute) { await _next(context); return; }
        var session = await sessions.ValidateAsync(context.Request.Headers.Authorization.ToString(), context.RequestAborted);
        if (session == null)
        {
            context.Response.StatusCode = 401;
            await context.Response.WriteAsJsonAsync(new { message = "Sign in again to continue." }, context.RequestAborted);
            return;
        }
        if (memberRoute && (!int.TryParse(context.Request.RouteValues["memberId"]?.ToString(),
                NumberStyles.None, CultureInfo.InvariantCulture, out int memberId) || memberId != session.MemberId))
        {
            context.Response.StatusCode = 403;
            await context.Response.WriteAsJsonAsync(new { message = "This member account is not authorised." }, context.RequestAborted);
            return;
        }
        context.Items[MemberLoginSessionService.ContextKey] = session;
        await _next(context);
    }
}