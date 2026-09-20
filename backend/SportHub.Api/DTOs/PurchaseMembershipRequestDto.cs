namespace SportHub.Api.DTOs;

public class PurchaseMembershipRequestDto
{
    public int MembershipPlanId { get; set; }

    public string OperationId { get; set; } =
        string.Empty;
}