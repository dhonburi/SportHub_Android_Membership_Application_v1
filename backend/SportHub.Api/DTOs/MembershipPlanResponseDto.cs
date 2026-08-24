namespace SportHub.Api.DTOs;

public class MembershipPlanResponseDto
{
    public int MembershipPlanId { get; set; }

    public string PlanName { get; set; } = string.Empty;

    public decimal Price { get; set; }

    public string? Description { get; set; }

    public bool IsAlreadyActive { get; set; }
}