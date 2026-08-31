using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.Models;
using SportHub.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Add controller support.
builder.Services.AddControllers();

// Add Swagger/OpenAPI support.
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure Entity Framework Core to use the connection string
// named "SportHubDatabase".
builder.Services.AddDbContext<SportHubDbContext>(
    options =>
        options.UseSqlServer(
            builder.Configuration.GetConnectionString(
                "SportHubDatabase"
            )
        )
);

// Register the password hasher used for user passwords.
builder.Services.AddScoped
    IPasswordHasher<User>,
    PasswordHasher<User>
>();

// Register the authentication service.
builder.Services.AddScoped<AuthService>();

// Register the shared QR token service (US-09 issuance,
// US-10 validation). Singleton: holds only an immutable
// signing key read once from configuration, no DbContext
// dependency.
builder.Services.AddSingleton<QrTokenService>();

var app = builder.Build();

/*
 * Running:
 *
 * dotnet run -- --seed
 *
 * creates the fake development account and exits.
 * A normal "dotnet run" does not seed automatically.
 */
bool seedRequested =
    args.Any(
        argument =>
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
        scope.ServiceProvider
            .GetRequiredService
                IPasswordHasher<User>
            >();

    // Apply any pending migrations before seeding.
    await dbContext.Database.MigrateAsync();

    // Insert the test member and user account if they do not exist.
    await DbSeeder.SeedAsync(
        dbContext,
        passwordHasher
    );

    return;
}

// Enable Swagger in both local development and Azure.
// This lets you access /swagger after deployment.
app.UseSwagger();
app.UseSwaggerUI();

/*
 * Local Android testing may use HTTP.
 * Azure App Service uses HTTPS.
 */
if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseAuthorization();

app.MapControllers();

app.Run();