namespace SportHub.Api.Models;

public class MembershipPlan
{
    public int MembershipPlanId { get; set; }

    public string PlanName { get; set; } = string.Empty;

    public decimal Price { get; set; }

    public string? Description { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<MemberMembership> MemberMemberships { get; set; } =
        new List<MemberMembership>();
}