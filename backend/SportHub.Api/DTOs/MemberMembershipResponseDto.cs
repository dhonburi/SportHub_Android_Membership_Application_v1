namespace SportHub.Api.DTOs;

public class MemberMembershipResponseDto
{
    public int MemberMembershipId { get; set; }

    public string MemberNumber { get; set; } = string.Empty;

    public string PlanName { get; set; } = string.Empty;

    public decimal Price { get; set; }

    public string? Description { get; set; }

    public string Status { get; set; } = string.Empty;

    public DateTime StartDate { get; set; }

    public DateTime? ExpiryDate { get; set; }

    public int? RemainingEntries { get; set; }
}