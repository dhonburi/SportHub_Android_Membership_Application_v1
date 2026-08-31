namespace SportHub.Api.DTOs;

public class BalanceQrCodeResponseDto
{
    public int MemberId { get; set; }

    public decimal Balance { get; set; }

    public string Currency { get; set; } = string.Empty;

    public string QrToken { get; set; } = string.Empty;

    public string IssuedAtUtc { get; set; } = string.Empty;

    public string ExpiresAtUtc { get; set; } = string.Empty;

    public int ValiditySeconds { get; set; }
}