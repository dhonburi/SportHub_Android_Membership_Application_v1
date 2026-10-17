namespace SportHub.Api.DTOs;

public class RegisterResponseDto
{
    public bool Success { get; set; }
    public string Code { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public int? MemberId { get; set; }
    public string? MemberNumber { get; set; }
    public Dictionary<string, string[]> Errors { get; set; } = new();
}