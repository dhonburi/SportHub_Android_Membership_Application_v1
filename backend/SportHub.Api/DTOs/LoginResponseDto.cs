namespace SportHub.Api.DTOs;

public class LoginResponseDto
{
    public bool Success { get; set; }

    public int? UserId { get; set; }

    public int? MemberId { get; set; }

    public string? MemberNumber { get; set; }

    public string Message { get; set; } = string.Empty;
}