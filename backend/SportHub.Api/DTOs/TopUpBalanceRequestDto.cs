namespace SportHub.Api.DTOs;

public class TopUpBalanceRequestDto
{
    public decimal Amount { get; set; }

    public string OperationId { get; set; } =
        string.Empty;
}