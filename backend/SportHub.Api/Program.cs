using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using SportHub.Api.Data;
using SportHub.Api.Models;
using SportHub.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Controllers and Swagger.
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Database.
builder.Services.AddDbContext<SportHubDbContext>(
    options =>
        options.UseSqlServer(
            builder.Configuration.GetConnectionString(
                "SportHubDatabase"
            )
        )
);

// Existing authentication and QR services.
builder.Services.AddScoped<
    IPasswordHasher<User>,
    PasswordHasher<User>
>();

builder.Services.AddScoped<AuthService>();
builder.Services.AddSingleton<QrTokenService>();

// Staff tokens must use their own secret, separate from QR tokens.
var staffTokenService =
    new StaffTokenService(builder.Configuration);

builder.Services.AddSingleton(staffTokenService);

builder.Services
    .AddAuthentication(
        JwtBearerDefaults.AuthenticationScheme
    )
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters =
            new TokenValidationParameters
            {
                ValidateIssuer = true,
                ValidIssuer = StaffTokenService.Issuer,

                ValidateAudience = true,
                ValidAudience = StaffTokenService.Audience,

                ValidateIssuerSigningKey = true,
                IssuerSigningKey =
                    staffTokenService.SigningKey,

                ValidateLifetime = true,
                ClockSkew = TimeSpan.FromSeconds(30)
            };
    });

builder.Services.AddAuthorization();

var app = builder.Build();

/*
 * Running `dotnet run -- --seed` applies migrations,
 * creates the development account if needed, and exits.
 */
bool seedRequested =
    args.Any(argument =>
        string.Equals(
            argument,
            "--seed",
            StringComparison.OrdinalIgnoreCase
        )
    );

if (seedRequested)
{
    using IServiceScope scope =
        app.Services.CreateScope();

    SportHubDbContext dbContext =
        scope.ServiceProvider
            .GetRequiredService<SportHubDbContext>();

    IPasswordHasher<User> passwordHasher =
        scope.ServiceProvider.GetRequiredService<
            IPasswordHasher<User>
        >();

    await dbContext.Database.MigrateAsync();

    await DbSeeder.SeedAsync(
        dbContext,
        passwordHasher
    );

    return;
}

app.UseSwagger();
app.UseSwaggerUI();

if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

// Protect the existing gate controller without replacing
// its large file again.
app.UseMiddleware<
    GateStaffAuthorizationMiddleware
>();

app.MapControllers();

app.Run();