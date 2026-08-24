namespace SportHub.Api.DTOs;

public class TopUpBalanceResponseDto
{
    public int MemberId { get; set; }

    public decimal AmountAdded { get; set; }

    public decimal Balance { get; set; }

    public string Currency { get; set; } = "NZD";

    public string Message { get; set; } = string.Empty;
}