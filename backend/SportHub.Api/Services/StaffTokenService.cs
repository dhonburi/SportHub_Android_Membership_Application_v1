using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.IdentityModel.Tokens;

namespace SportHub.Api.Services;

public sealed class StaffTokenService
{
    public const string Issuer = "SportHub.Api";
    public const string Audience = "SportHub.Staff";

    private static readonly TimeSpan TokenLifetime =
        TimeSpan.FromMinutes(30);

    private readonly SigningCredentials _signingCredentials;

    public StaffTokenService(IConfiguration configuration)
    {
        string? encodedKey =
            configuration["StaffAuth:SigningKey"];

        if (string.IsNullOrWhiteSpace(encodedKey))
        {
            throw new InvalidOperationException(
                "StaffAuth:SigningKey is not configured."
            );
        }

        byte[] keyBytes;

        try
        {
            keyBytes = Convert.FromBase64String(encodedKey);
        }
        catch (FormatException exception)
        {
            throw new InvalidOperationException(
                "StaffAuth:SigningKey must be a Base64-encoded key.",
                exception
            );
        }

        if (keyBytes.Length < 32)
        {
            throw new InvalidOperationException(
                "StaffAuth:SigningKey must contain at least 32 bytes."
            );
        }

        SigningKey = new SymmetricSecurityKey(keyBytes);

        _signingCredentials = new SigningCredentials(
            SigningKey,
            SecurityAlgorithms.HmacSha256
        );
    }

    public SymmetricSecurityKey SigningKey { get; }

    public (string Token, DateTime ExpiresAtUtc)
        CreateAdminToken(int userId)
    {
        DateTime issuedAtUtc = DateTime.UtcNow;
        DateTime expiresAtUtc =
            issuedAtUtc.Add(TokenLifetime);

        Claim[] claims =
        {
            new(
                ClaimTypes.NameIdentifier,
                userId.ToString(
                    System.Globalization.CultureInfo.InvariantCulture
                )
            ),
            new(ClaimTypes.Role, "Admin"),
            new(
                JwtRegisteredClaimNames.Jti,
                Guid.NewGuid().ToString("D")
            )
        };

        var jwt = new JwtSecurityToken(
            issuer: Issuer,
            audience: Audience,
            claims: claims,
            notBefore: issuedAtUtc,
            expires: expiresAtUtc,
            signingCredentials: _signingCredentials
        );

        string token =
            new JwtSecurityTokenHandler()
                .WriteToken(jwt);

        return (token, expiresAtUtc);
    }
}