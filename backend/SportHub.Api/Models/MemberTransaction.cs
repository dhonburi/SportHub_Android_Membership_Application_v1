namespace SportHub.Api.Models;

public class MemberTransaction
{
    public const string BalanceTopUpType = "BalanceTopUp";

    public const string MembershipPurchaseType =
        "MembershipPurchase";

    public int TransactionId { get; set; }

    public int MemberId { get; set; }

    public int? MemberMembershipId { get; set; }

    public string OperationId { get; set; } = string.Empty;

    public string TransactionType { get; set; } = string.Empty;

    public string Description { get; set; } = string.Empty;

    public decimal Amount { get; set; }

    public decimal BalanceAfter { get; set; }

    public DateTime OccurredAtUtc { get; set; } =
        DateTime.UtcNow;

    public Member Member { get; set; } = null!;

    public MemberMembership? MemberMembership { get; set; }
}