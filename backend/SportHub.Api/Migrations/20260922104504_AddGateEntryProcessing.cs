using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SportHub.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddGateEntryProcessing : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "GateEntryProcessings",
                columns: table => new
                {
                    GateEntryProcessingId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    ProcessingId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TokenHash = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: false),
                    MemberId = table.Column<int>(type: "int", nullable: false),
                    MemberMembershipId = table.Column<int>(type: "int", nullable: true),
                    AccessMethod = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false),
                    AmountCharged = table.Column<decimal>(type: "decimal(10,2)", precision: 10, scale: 2, nullable: true),
                    EntriesDeducted = table.Column<int>(type: "int", nullable: true),
                    BalanceAfter = table.Column<decimal>(type: "decimal(10,2)", precision: 10, scale: 2, nullable: false),
                    RemainingEntriesAfter = table.Column<int>(type: "int", nullable: true),
                    Status = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false),
                    ProcessedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_GateEntryProcessings", x => x.GateEntryProcessingId);
                    table.CheckConstraint("CK_GateEntryProcessings_AmountCharged", "[AmountCharged] IS NULL OR [AmountCharged] >= 0");
                    table.CheckConstraint("CK_GateEntryProcessings_EntriesDeducted", "[EntriesDeducted] IS NULL OR [EntriesDeducted] >= 0");
                    table.ForeignKey(
                        name: "FK_GateEntryProcessings_MemberMemberships_MemberMembershipId",
                        column: x => x.MemberMembershipId,
                        principalTable: "MemberMemberships",
                        principalColumn: "MemberMembershipId",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_GateEntryProcessings_Members_MemberId",
                        column: x => x.MemberId,
                        principalTable: "Members",
                        principalColumn: "MemberId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_GateEntryProcessings_MemberId_ProcessedAtUtc",
                table: "GateEntryProcessings",
                columns: new[] { "MemberId", "ProcessedAtUtc" });

            migrationBuilder.CreateIndex(
                name: "IX_GateEntryProcessings_MemberMembershipId",
                table: "GateEntryProcessings",
                column: "MemberMembershipId");

            migrationBuilder.CreateIndex(
                name: "IX_GateEntryProcessings_ProcessingId",
                table: "GateEntryProcessings",
                column: "ProcessingId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_GateEntryProcessings_TokenHash",
                table: "GateEntryProcessings",
                column: "TokenHash",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "GateEntryProcessings");
        }
    }
}
