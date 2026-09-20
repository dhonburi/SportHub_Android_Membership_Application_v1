namespace SportHub.Api.DTOs;

public class MemberTransactionResponseDto
{
    public int TransactionId { get; set; }

    public int MemberId { get; set; }

    public string TransactionType { get; set; } =
        string.Empty;

    public string Description { get; set; } =
        string.Empty;

    public decimal Amount { get; set; }

    public decimal BalanceAfter { get; set; }

    public string Currency { get; set; } = "NZD";

    public string OccurredAtUtc { get; set; } =
        string.Empty;
}