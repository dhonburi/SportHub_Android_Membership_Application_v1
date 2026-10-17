namespace SportHub.Api.DTOs;

public class PurchaseMembershipResponseDto
{
    public int MemberMembershipId { get; set; }

    public int MemberId { get; set; }

    public int MembershipPlanId { get; set; }

    public string PlanName { get; set; } = string.Empty;

    public decimal PricePaid { get; set; }

    public decimal Balance { get; set; }

    public string Currency { get; set; } = "NZD";

    public string Status { get; set; } = string.Empty;

    public DateTime StartDate { get; set; }

    public DateTime? ExpiryDate { get; set; }

    public int? RemainingEntries { get; set; }

    public string Message { get; set; } = string.Empty;
}