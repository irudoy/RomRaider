# UDP external data source

`com.romraider.logger.external.udp.plugin.UdpDataSource` feeds RomRaider Logger
with values that another program sends as UDP datagrams. It serves data
acquisition paths the Logger has no transport for, such as a CAN telemetry
stream decoded by a separate application. The values appear as external
parameters, and a table axis that names such a parameter in `logparam` follows
it with `Overlay Log`.

## Plugin file

A file with the `.plugin` extension declares the listening endpoint and the
data items. The Logger reads plugin files from two folders when it starts:

| Folder | Purpose |
| --- | --- |
| `plugins` in the installation folder | plugin files RomRaider ships |
| `plugins` in the per-user folder, `~/.RomRaider/plugins` (`%USERPROFILE%\.RomRaider\plugins` on Windows) | plugin files another program writes; they stay in place when the installation is replaced |

A per-user file replaces an installation file of the same name. The sending
program owns the channel table, so it writes the plugin file:

```properties
datasource.class=com.romraider.logger.external.udp.plugin.UdpDataSource
udp.name=My Telemetry
udp.host=127.0.0.1
udp.port=47809
udp.item.1=rpm|Engine Speed|RPM|0|0|8000|1000
udp.item.2=load|Engine Load|%|0.00|0|200|20
```

| Property | Meaning | Default |
| --- | --- | --- |
| `udp.name` | Source name in the Plugins menu | `UDP Telemetry` |
| `udp.host` | Local address to listen on | `127.0.0.1` |
| `udp.port` | UDP port; `0` selects a free port | `47809` |
| `udp.item.N` | `key|name|units|format|min|max|step` | none |
| `udp.item.N.expression` | expression that makes item `N` derived | none |
| `udp.item.N.table.VARIABLE` | name of the ROM table the expression reads as `VARIABLE` | none |

Items are ordered by `N`. `key` is the name of the value in the datagram,
`format` is a `DecimalFormat` pattern, and `min`, `max`, and `step` set the
gauge range. A malformed item or a repeated key rejects the plugin file. The
sender selects the endpoint, so the plugin file is its only source; the
Plugins menu entry reports the endpoint, the item count, and the derived
items.

The Logger derives a parameter ID from the item name as `X_` plus the name
with underscores for spaces. `Engine Speed` becomes `X_Engine_Speed`, the value
a ROM definition names in `logparam`.

## Derived items

An item with `udp.item.N.expression` takes its value from the expression, and
the datagram does not carry its key. It serves a value that depends on the
calibration, such as a table coordinate the ECU computes as a ratio of a
measured value to a ROM word. The sender states the arithmetic and RomRaider
supplies the ROM word, so the value follows the image open in the editor and
nobody copies a calibration value by hand:

```properties
udp.item.3=fuel_ms|Fuel Schedule|ms|0.000|0|20|2
udp.item.4=load|Engine Load|%|0.00|0|200|20
udp.item.4.expression=min(255, floor(round(fuel_ms*2048)*128/reference))*100/128
udp.item.4.table.reference=Load Reference
```

| Expression element | Meaning |
| --- | --- |
| item key, such as `fuel_ms` | last received value of a datagram item; another derived item is not a variable |
| table variable, such as `reference` | first cell of the named table as the ROM stores it, before the table scaling |
| `+ - * / % ^`, comparisons, `if(condition, a, b)`, `abs`, `sqrt` and the other standard functions | expression parser of the table scalings |
| `floor(x)`, `round(x)`, `min(a, b)`, `max(a, b)` | integer arithmetic of an ECU |

A table variable reads the image selected in the editor, then the other open
images in their order, and takes the first image that has the table. The value
follows the editor within half a second, including an edit that is not saved
yet. The stored value does not depend on the scaling selected for the table.

A derived item reads `0` while no open image has one of its tables or while
the expression value is not finite. This is the state of a Logger that runs
without the editor. The Plugins menu entry shows every derived item with its
expression and, for each table variable, the value and the image file it comes
from or the table name that no open image has.

An expression that does not parse, a table variable that repeats an item key,
a table binding without an expression, an expression without an item
definition, and any other `udp.item.N.` property reject the plugin file.

## Datagram

One ASCII datagram carries any number of values:

```text
RRUDP1 rpm=2512.5 load=43.75
```

The first token names the format and its version. Every other token is
`key=value` with a decimal value, separated by whitespace. A datagram with
another first token is ignored. A malformed token, an undeclared key, and a
value that is not finite are skipped, and an item keeps its last value until a
datagram carries its key again. The sender delivers engineering units; the
single convertor of an item passes the value through.

## Operation

The socket opens when the first item of the source is selected in the Logger
and closes when the last one is deselected. The Logger samples the last
received value on its own polling cycle. With no ECU module selected the
Logger polls external sources alone, so the data source works without an ECU
connection.
