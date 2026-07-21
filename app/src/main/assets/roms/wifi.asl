DefinitionBlock ("wifi.aml", "SSDT", 2, "QEMU", "WIFI", 0x00000001)
{
    External (\_SB.PCI0, DeviceObj)

    Scope (\_SB.PCI0)
    {
        Device (WIF1)
        {
            Name (_HID, "PRP0001")

            Name (_CID, Package () {
                "PCI\\VEN_8086&DEV_2723",
                "PNP0C02"
            })

            Name (_STR, Unicode ("Intel(R) Wi-Fi 6 AX200 160MHz"))

            Method (_STA, 0, NotSerialized)
            {
                Return (0x0F)
            }
        }
    }
}