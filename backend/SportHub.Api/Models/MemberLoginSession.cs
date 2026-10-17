namespace SportHub.Api.Models;

public class MemberLoginSession
{
    public Guid SessionId { get; set; } = Guid.NewGuid();
    public int UserId { get; set; }
    public int MemberId { get; set; }
    public string TokenHash { get; set; } = string.Empty;
    public string CredentialStamp { get; set; } = string.Empty;
    public DateTime IssuedAtUtc { get; set; }
    public DateTime ExpiresAtUtc { get; set; }
    public DateTime? RevokedAtUtc { get; set; }
    public User User { get; set; } = null!;
}