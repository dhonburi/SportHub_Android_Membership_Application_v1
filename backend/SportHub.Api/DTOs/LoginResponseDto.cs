namespace SportHub.Api.DTOs;

public class LoginResponseDto
{
    public bool Success { get; set; }

    public int? UserId { get; set; }

    public int? MemberId { get; set; }

    public string? MemberNumber { get; set; }

    public bool IsAdmin { get; set; }

    public string? StaffAccessToken { get; set; }

    public DateTime? StaffAccessTokenExpiresAtUtc { get; set; }

    public string? MemberAccessToken { get; set; }
    public long? MemberSessionExpiresAtUnixMs { get; set; }
    public long ServerTimeUnixMs { get; set; }

    public string Message { get; set; } = string.Empty;
}