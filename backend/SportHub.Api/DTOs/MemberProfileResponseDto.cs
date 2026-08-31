namespace SportHub.Api.DTOs;

public class MemberProfileResponseDto
{
    public int MemberId { get; set; }

    public string MemberNumber { get; set; } = string.Empty;

    public string FirstName { get; set; } = string.Empty;

    public string LastName { get; set; } = string.Empty;

    public string? Email { get; set; }

    public string? Phone { get; set; }

    public string? Gender { get; set; }

    public decimal Balance { get; set; }
}