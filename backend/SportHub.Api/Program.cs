using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Data;
using SportHub.Api.Models;
using SportHub.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();

builder.Services.AddEndpointsApiExplorer();

builder.Services.AddSwaggerGen();

builder.Services.AddDbContext<SportHubDbContext>(
    options =>
        options.UseSqlServer(
            builder.Configuration.GetConnectionString(
                "SportHubDatabase"
            )
        )
);

builder.Services.AddScoped<
    IPasswordHasher<User>,
    PasswordHasher<User>
>();

builder.Services.AddScoped<AuthService>();

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
            .GetRequiredService<
                IPasswordHasher<User>
            >();

    await dbContext.Database.MigrateAsync();

    await DbSeeder.SeedAsync(
        dbContext,
        passwordHasher
    );

    return;
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();

    app.UseSwaggerUI();
}

/*
 * Local Android testing will use development HTTP.
 * Production should use HTTPS.
 */
if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseAuthorization();

app.MapControllers();

app.Run();