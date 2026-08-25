using System.ComponentModel.DataAnnotations;

namespace SportHub.Api.DTOs;

public class UpdateMemberProfileRequestDto
{
    [Required(ErrorMessage = "First name is required.")]
    [StringLength(
        50,
        MinimumLength = 2,
        ErrorMessage = "First name must be between 2 and 50 characters."
    )]
    [RegularExpression(
        @"^[\p{L} ]+$",
        ErrorMessage = "First name must only contain letters and spaces."
    )]
    public string FirstName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Last name is required.")]
    [StringLength(
        50,
        MinimumLength = 2,
        ErrorMessage = "Last name must be between 2 and 50 characters."
    )]
    [RegularExpression(
        @"^[\p{L} ]+$",
        ErrorMessage = "Last name must only contain letters and spaces."
    )]
    public string LastName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email address is required.")]
    [StringLength(
        256,
        ErrorMessage = "Email address cannot exceed 256 characters."
    )]
    [RegularExpression(
        @"^[^\s@]+@[^\s@]+\.[^\s@]+$",
        ErrorMessage =
            "Email must include @ and a valid domain, such as name@example.com."
    )]
    public string Email { get; set; } = string.Empty;

    [RegularExpression(
        @"^\d{9,11}$",
        ErrorMessage = "Phone number must contain between 9 and 11 digits."
    )]
    public string? Phone { get; set; }

    [RegularExpression(
        @"^(Male|Female|Rather Not Say)$",
        ErrorMessage =
            "Gender must be Male, Female or Rather Not Say."
    )]
    public string? Gender { get; set; }
}