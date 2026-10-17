 using Microsoft.AspNetCore.Mvc;
 using Microsoft.EntityFrameworkCore;
 using Microsoft.Extensions.Configuration;
 using SportHub.Api.Data;
 using SportHub.Api.DTOs;
 using SportHub.Api.Services;

 namespace SportHub.Api.Controllers;

 [ApiController]
 [Route("api/membership-qr")]
 public class MembershipQrController : ControllerBase
 {
     private readonly SportHubDbContext _dbContext;

     private readonly QrTokenService _qrTokenService;

     private readonly IConfiguration _configuration;

     public MembershipQrController(
         SportHubDbContext dbContext,
         QrTokenService qrTokenService,
         IConfiguration configuration
     )
     {
         _dbContext = dbContext;
         _qrTokenService = qrTokenService;
         _configuration = configuration;
     }

     [HttpPost("validate")]
     public async Task<
         ActionResult<MembershipQrValidationResponseDto>
     > ValidateMembershipQr(
         MembershipQrValidationRequestDto? request
     )
     {
         QrTokenValidationResult tokenResult =
             _qrTokenService.ValidateToken(
                 request?.QrToken
             );

         if (tokenResult.Status ==
             QrTokenValidationStatus.Missing)
         {
             return Ok(CreateResult(
                 false,
                 "MISSING_TOKEN",
                 "Access Denied: a QR token is required."
             ));
         }

         if (tokenResult.Status ==
             QrTokenValidationStatus.Malformed)
         {
             return Ok(CreateResult(
                 false,
                 "MALFORMED_TOKEN",
                 "Access Denied: this is not a supported SportHub QR code."
             ));
         }

         if (tokenResult.Status ==
             QrTokenValidationStatus.InvalidSignature)
         {
             return Ok(CreateResult(
                 false,
                 "INVALID_SIGNATURE",
                 "Access Denied: the QR token signature is invalid."
             ));
         }

         if (tokenResult.Status ==
             QrTokenValidationStatus.Expired)
         {
             return Ok(CreateResult(
                 false,
                 "EXPIRED_TOKEN",
                 "Access Denied: this QR token has expired. Scan the refreshed code."
             ));
         }

         if (tokenResult.Type ==
             QrTokenType.Membership)
         {
             MembershipQrValidationResponseDto
                 membershipResult =
                     await ValidateMembershipAsync(
                         tokenResult.Id
                     );

             return Ok(membershipResult);
         }

         MembershipQrValidationResponseDto
             balanceResult =
                 await ValidateBalanceAccessAsync(
                     tokenResult.Id
                 );

         return Ok(balanceResult);
     }

     private async Task<
         MembershipQrValidationResponseDto
     > ValidateMembershipAsync(
         int memberMembershipId
     )
     {
         var membership =
             await _dbContext.MemberMemberships
                 .AsNoTracking()
                 .Where(existingMembership =>
                     existingMembership
                         .MemberMembershipId ==
                     memberMembershipId
                 )
                 .Select(existingMembership => new
                 {
                     existingMembership
                         .Member
                         .MemberNumber,

                     existingMembership
                         .MembershipPlan
                         .PlanName,

                     existingMembership.Status,

                     existingMembership.ExpiryDate,

                     existingMembership.RemainingEntries
                 })
                 .SingleOrDefaultAsync();

         if (membership == null)
         {
             return CreateResult(
                 false,
                 "MEMBERSHIP_NOT_FOUND",
                 "Access Denied: the membership does not exist.",
                 "MEMBERSHIP"
             );
         }

         DateTime today =
             GetNewZealandToday();

         bool isExpired =
             string.Equals(
                 membership.Status,
                 "Expired",
                 StringComparison.OrdinalIgnoreCase
             )
             || (
                 membership.ExpiryDate.HasValue
                 && membership
                     .ExpiryDate
                     .Value
                     .Date < today
             );

         if (isExpired)
         {
             return CreateResult(
                 false,
                 "EXPIRED_MEMBERSHIP",
                 "Access Denied: this membership has expired.",
                 "MEMBERSHIP",
                 membership.MemberNumber,
                 membership.PlanName,
                 membership.Status,
                 membership.RemainingEntries
             );
         }

         bool isActive =
             string.Equals(
                 membership.Status,
                 "Active",
                 StringComparison.OrdinalIgnoreCase
             );

         if (!isActive)
         {
             return CreateResult(
                 false,
                 "INACTIVE_MEMBERSHIP",
                 "Access Denied: this membership is not active.",
                 "MEMBERSHIP",
                 membership.MemberNumber,
                 membership.PlanName,
                 membership.Status,
                 membership.RemainingEntries
             );
         }

         if (membership.RemainingEntries.HasValue
             && membership.RemainingEntries.Value <= 0)
         {
             return CreateResult(
                 false,
                 "NO_REMAINING_ENTRIES",
                 "Access Denied: this passcard has no remaining entries.",
                 "MEMBERSHIP",
                 membership.MemberNumber,
                 membership.PlanName,
                 membership.Status,
                 membership.RemainingEntries
             );
         }

         return CreateResult(
             true,
             "VALID",
             "Access Approved",
             "MEMBERSHIP",
             membership.MemberNumber,
             membership.PlanName,
             membership.Status,
             membership.RemainingEntries
         );
     }

     private async Task<
         MembershipQrValidationResponseDto
     > ValidateBalanceAccessAsync(
         int memberId
     )
     {
         var member =
             await _dbContext.Members
                 .AsNoTracking()
                 .Where(existingMember =>
                     existingMember.MemberId ==
                     memberId
                 )
                 .Select(existingMember => new
                 {
                     existingMember.MemberNumber,
                     existingMember.Balance
                 })
                 .SingleOrDefaultAsync();

         if (member == null)
         {
             return CreateResult(
                 false,
                 "MEMBER_NOT_FOUND",
                 "Access Denied: the member does not exist.",
                 "BALANCE"
             );
         }

         decimal minimumBalance =
             _configuration.GetValue(
                 "Qr:MinimumBalanceForBalanceQrCode",
                 0m
             );

         if (member.Balance <= minimumBalance)
         {
             return CreateResult(
                 false,
                 "INSUFFICIENT_BALANCE",
                 "Access Denied: the member does not have enough balance.",
                 "BALANCE",
                 member.MemberNumber
             );
         }

         return CreateResult(
             true,
             "VALID",
             "Access Approved",
             "BALANCE",
             member.MemberNumber
         );
     }

     private static DateTime GetNewZealandToday()
     {
         TimeZoneInfo newZealandTimeZone =
             TimeZoneInfo.FindSystemTimeZoneById(
                 "Pacific/Auckland"
             );

         return TimeZoneInfo.ConvertTimeFromUtc(
             DateTime.UtcNow,
             newZealandTimeZone
         ).Date;
     }

     private static MembershipQrValidationResponseDto
         CreateResult(
             bool isApproved,
             string resultCode,
             string message,
             string? accessType = null,
             string? memberNumber = null,
             string? planName = null,
             string? membershipStatus = null,
             int? remainingEntries = null
         )
     {
         return new MembershipQrValidationResponseDto
         {
             IsApproved = isApproved,

             Decision = isApproved
                 ? "APPROVED"
                 : "DENIED",

             ResultCode = resultCode,
             Message = message,
             AccessType = accessType,
             MemberNumber = memberNumber,
             PlanName = planName,
             MembershipStatus = membershipStatus,
             RemainingEntries = remainingEntries
         };
     }
 }