namespace SportHub.Api.Models;

public class GateEntryProcessing
{
    public int GateEntryProcessingId { get; set; }

    public Guid ProcessingId { get; set; } = Guid.NewGuid();

    public string TokenHash { get; set; } = string.Empty;

    public int MemberId { get; set; }

    public int? MemberMembershipId { get; set; }

    public string AccessMethod { get; set; } = string.Empty;

    public decimal? AmountCharged { get; set; }

    public int? EntriesDeducted { get; set; }

    public decimal BalanceAfter { get; set; }

    public int? RemainingEntriesAfter { get; set; }

    public string Status { get; set; } = "Completed";

    public DateTime ProcessedAtUtc { get; set; } = DateTime.UtcNow;

    public Member Member { get; set; } = null!;

    public MemberMembership? MemberMembership { get; set; }
}