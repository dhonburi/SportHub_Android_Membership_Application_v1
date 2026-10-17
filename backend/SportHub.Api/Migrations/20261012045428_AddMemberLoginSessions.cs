using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SportHub.Api.Migrations
{
    public partial class AddMemberLoginSessions : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "MemberLoginSessions",
                columns: table => new
                {
                    SessionId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<int>(type: "int", nullable: false),
                    MemberId = table.Column<int>(type: "int", nullable: false),
                    TokenHash = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: false),
                    CredentialStamp = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: false),
                    IssuedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    ExpiresAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RevokedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_MemberLoginSessions", x => x.SessionId);
                    table.ForeignKey(name: "FK_MemberLoginSessions_Users_UserId", column: x => x.UserId,
                        principalTable: "Users", principalColumn: "UserId", onDelete: ReferentialAction.Cascade);
                });
            migrationBuilder.CreateIndex("IX_MemberLoginSessions_ExpiresAtUtc", "MemberLoginSessions", "ExpiresAtUtc");
            migrationBuilder.CreateIndex("IX_MemberLoginSessions_TokenHash", "MemberLoginSessions", "TokenHash", unique: true);
            migrationBuilder.CreateIndex("IX_MemberLoginSessions_UserId", "MemberLoginSessions", "UserId");
        }
        protected override void Down(MigrationBuilder migrationBuilder) => migrationBuilder.DropTable("MemberLoginSessions");
    }
}