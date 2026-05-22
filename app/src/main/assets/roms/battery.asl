DefinitionBlock ("battery.aml", "SSDT", 2, "QEMU", "BATTOS", 0x00000001)
{
    Scope (\_SB)
    {

        Device (EC0)
        {
            Name (_HID, EisaId ("PNP0C09"))

            Method (_STA, 0, NotSerialized)
            {
                Return (0x0F)
            }
        }

        Device (AC)
        {
            Name (_HID, "ACPI0003")

            Method (_STA, 0, NotSerialized)
            {
                Return (0x0F)
            }

            Method (_PSR, 0, NotSerialized)
            {
                Return (One) // Charging AC
            }
        }

        Device (BAT0)
        {
            Name (_HID, EisaId ("PNP0C0A"))
            Name (_UID, One)

            Name (_PCL, Package ()
            {
                \_SB
            })

            Method (_STA, 0, NotSerialized)
            {
                Return (0x1F)
            }

            Method (_BIF, 0, NotSerialized)
            {
                Return (Package ()
                {
                    One,        // Power Unit = mAh
                    9000,      // Design Capacity
                    ${last-full-charge-capacity},      // Last Full Charge Capacity
                    One,        // Rechargeable
                    11111,      // Design Voltage (11.111V)
                    400,        // Warning Capacity
                    200,        // Low Capacity
                    100,
                    100,
                    "99Wh Laptop Battery",
                    "202605",
                    "LiIon",
                    "VECTRAS_VM_BATTERY_EMULATOR"
                })
            }

            Method (_BST, 0, NotSerialized)
            {
                Return (Package ()
                {
                    ${charging-status},    // Charging
                    500,     // Charge rate
                    ${remaining-capacity},   // Remaining capacity
                    12600    // Current voltage
                })
            }
        }
    }
}