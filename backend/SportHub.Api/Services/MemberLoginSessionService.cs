using System.Security.Cryptography;
using System.Text;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.Models;

namespace SportHub.Api.Services;

public sealed class MemberLoginSessionService
{
    public const string ContextKey = "SportHub.MemberSession";
    private readonly SportHubDbContext _db;
    public MemberLoginSessionService(SportHubDbContext db) => _db = db;
    private static string Hash(string value) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(value)));

    public async Task<(string Token, MemberLoginSession Session)> CreateAsync(int userId, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        // AuthService and this service share one request-scoped DbContext.
        // Reuse the account whose password was just verified instead of making
        // a second SQL read. Never cache it across requests or PIN unlocks.
        var user = _db.Users.Local.SingleOrDefault(u => u.UserId == userId);
        if (user == null)
            user = await _db.Users.SingleAsync(u => u.UserId == userId && u.IsActive, cancellationToken);
        if (!user.IsActive)
            throw new InvalidOperationException("An inactive account cannot create a member session.");
        var now = DateTime.UtcNow;
        var token = Convert.ToHexString(RandomNumberGenerator.GetBytes(32));
        var session = new MemberLoginSession
        {
            UserId = user.UserId, MemberId = user.MemberId, TokenHash = Hash(token),
            CredentialStamp = Hash(user.PasswordHash), IssuedAtUtc = now, ExpiresAtUtc = now.AddDays(7)
        };
        _db.MemberLoginSessions.Add(session);
        await _db.SaveChangesAsync(cancellationToken);
        return (token, session);
    }

    public async Task<MemberLoginSession?> ValidateAsync(string? authorization, CancellationToken cancellationToken)
    {
        if (authorization == null || !authorization.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase)) return null;
        var token = authorization[7..].Trim();
        if (token.Length != 64 || token.Any(c => !Uri.IsHexDigit(c))) return null;
        var hash = Hash(token);
        var session = await _db.MemberLoginSessions.AsNoTracking().Include(s => s.User)
            .SingleOrDefaultAsync(s => s.TokenHash == hash, cancellationToken);
        if (session == null || session.RevokedAtUtc != null || DateTime.UtcNow >= session.ExpiresAtUtc
            || !session.User.IsActive || session.User.MemberId != session.MemberId
            || !string.Equals(session.CredentialStamp, Hash(session.User.PasswordHash), StringComparison.Ordinal)) return null;
        return session;
    }

    public async Task RevokeAsync(Guid sessionId, CancellationToken cancellationToken)
    {
        await _db.MemberLoginSessions.Where(s => s.SessionId == sessionId)
            .ExecuteUpdateAsync(update => update.SetProperty(s => s.RevokedAtUtc, DateTime.UtcNow), cancellationToken);
    }
}
