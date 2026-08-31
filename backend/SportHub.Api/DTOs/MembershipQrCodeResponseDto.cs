namespace SportHub.Api.DTOs;

public class MembershipQrCodeResponseDto
{
    public int MemberMembershipId { get; set; }

    public string PlanName { get; set; } = string.Empty;

    public string Status { get; set; } = string.Empty;

    public string QrToken { get; set; } = string.Empty;

    public string IssuedAtUtc { get; set; } = string.Empty;

    public string ExpiresAtUtc { get; set; } = string.Empty;

    public int ValiditySeconds { get; set; }
}