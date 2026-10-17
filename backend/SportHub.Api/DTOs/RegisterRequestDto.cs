using System.ComponentModel.DataAnnotations;
using System.Text;

namespace SportHub.Api.DTOs;

public class RegisterRequestDto : IValidatableObject
{
    private string _firstName = string.Empty;
    private string _lastName = string.Empty;
    private string _email = string.Empty;
    private string? _phone;
    private string? _gender;

    // A small MVP blocklist, not a comprehensive breached-password database.
    private static readonly HashSet<string> BlockedPasswords =
        new(StringComparer.OrdinalIgnoreCase)
        {
            "passwordpassword", "password123456789",
            "123456789012345", "12345678901234567890",
            "qwertyuiopasdfgh", "qwertyuiopasdfghjkl",
            "letmeinletmeinletmein", "welcome123456789",
            "iloveyouiloveyou", "sporthub123456789",
            "sporthubpassword", "sporthubpassword123",
            "synergysports123", "synergysportspassword"
        };

    [Required(ErrorMessage = "First name is required.")]
    [StringLength(50, MinimumLength = 2,
        ErrorMessage = "First name must be between 2 and 50 characters.")]
    [RegularExpression(@"^[\p{L} ]+$",
        ErrorMessage = "First name must only contain letters and spaces.")]
    public string FirstName
    {
        get => _firstName;
        set => _firstName = (value ?? string.Empty).Trim();
    }

    [Required(ErrorMessage = "Last name is required.")]
    [StringLength(50, MinimumLength = 2,
        ErrorMessage = "Last name must be between 2 and 50 characters.")]
    [RegularExpression(@"^[\p{L} ]+$",
        ErrorMessage = "Last name must only contain letters and spaces.")]
    public string LastName
    {
        get => _lastName;
        set => _lastName = (value ?? string.Empty).Trim();
    }

    [Required(ErrorMessage = "Email address is required.")]
    [StringLength(256,
        ErrorMessage = "Email address cannot exceed 256 characters.")]
    [EmailAddress(ErrorMessage = "Enter a valid email address.")]
    [RegularExpression(@"^[^\s@]+@[^\s@]+\.[^\s@]+$",
        ErrorMessage = "Enter an email address such as name@example.com.")]
    public string Email
    {
        get => _email;
        set => _email = (value ?? string.Empty).Trim().ToLowerInvariant();
    }

    [RegularExpression(@"^[0-9]{9,11}$",
        ErrorMessage = "Phone number must contain between 9 and 11 digits.")]
    public string? Phone
    {
        get => _phone;
        set => _phone = string.IsNullOrWhiteSpace(value) ? null : value.Trim();
    }

    [RegularExpression(@"^(Male|Female|Rather Not Say)$",
        ErrorMessage = "Choose Male, Female or Rather Not Say.")]
    public string? Gender
    {
        get => _gender;
        set => _gender = string.IsNullOrWhiteSpace(value) ? null : value.Trim();
    }

    public DateTime? DateOfBirth { get; set; }

    // Never trim or change the password: spaces and case are significant.
    [Required(ErrorMessage = "Password is required.")]
    public string Password { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please confirm your password.")]
    [Compare(nameof(Password), ErrorMessage = "Passwords must match exactly.")]
    public string ConfirmPassword { get; set; } = string.Empty;

    public IEnumerable<ValidationResult> Validate(ValidationContext context)
    {
        if (DateOfBirth.HasValue && DateOfBirth.Value.Date > DateTime.UtcNow.Date)
        {
            yield return new ValidationResult(
                "Date of birth cannot be in the future.",
                new[] { nameof(DateOfBirth) });
        }

        if (string.IsNullOrWhiteSpace(Password))
            yield break;

        // Count Unicode code points so an emoji is not counted twice.
        int length = Password.EnumerateRunes().Count();

        if (length < 15 || length > 128)
        {
            yield return new ValidationResult(
                "Use a password or passphrase between 15 and 128 characters.",
                new[] { nameof(Password) });
            yield break;
        }

        if (BlockedPasswords.Contains(Password) ||
            Password.EnumerateRunes().Distinct().Count() == 1 ||
            string.Equals(Password, Email, StringComparison.OrdinalIgnoreCase))
        {
            yield return new ValidationResult(
                "This password is too predictable. Choose a different passphrase.",
                new[] { nameof(Password) });
        }
    }
}