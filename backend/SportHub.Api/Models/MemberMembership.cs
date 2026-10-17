namespace SportHub.Api.Models;

public class MemberMembership
{
    public int MemberMembershipId { get; set; }

    public int MemberId { get; set; }

    public int MembershipPlanId { get; set; }

    public string Status { get; set; } = string.Empty;

    public DateTime StartDate { get; set; }

    public DateTime? ExpiryDate { get; set; }

    public int? RemainingEntries { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public Member Member { get; set; } = null!;

    public MembershipPlan MembershipPlan { get; set; } = null!;
}