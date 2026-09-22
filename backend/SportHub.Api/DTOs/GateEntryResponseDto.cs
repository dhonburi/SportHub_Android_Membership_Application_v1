namespace SportHub.Api.DTOs;

public class GateEntryResponseDto
{
    public bool IsApproved { get; set; }

    public bool IsDuplicate { get; set; }

    public string Decision { get; set; } = string.Empty;

    public string ResultCode { get; set; } = string.Empty;

    public string Message { get; set; } = string.Empty;

    public string? ProcessingId { get; set; }

    public string? AccessType { get; set; }

    public string? MemberNumber { get; set; }

    public string? PlanName { get; set; }

    public string? MembershipStatus { get; set; }

    public int? RemainingEntries { get; set; }

    public decimal? AmountCharged { get; set; }

    public decimal? Balance { get; set; }

    public string Currency { get; set; } = "NZD";

    public string? ProcessedAtUtc { get; set; }
}