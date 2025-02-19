package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PlayerPublicKey;
import net.md_5.bungee.protocol.Property;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PlayerListItem extends DefinedPacket
{

    private Action action;
    private Item[] items;

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_8 )
        {
            items = new Item[ 1 ];
            Item item = items[ 0 ] = new Item();
            item.displayName = item.username = readString( buf );
            action = !buf.readBoolean() ? Action.REMOVE_PLAYER : Action.ADD_PLAYER;
            item.ping = buf.readShort();
        } else
        {
            action = Action.values()[ DefinedPacket.readVarInt( buf )];
            items = new Item[ DefinedPacket.readVarInt( buf ) ];
            for ( int i = 0; i < items.length; i++ )
            {
                Item item = items[ i ] = new Item();
                item.setUuid( DefinedPacket.readUUID( buf ) );
                switch ( action )
                {
                    case ADD_PLAYER:
                        item.username = DefinedPacket.readString( buf );
                        item.properties = DefinedPacket.readProperties( buf );
                        item.gamemode = DefinedPacket.readVarInt( buf );
                        item.ping = DefinedPacket.readVarInt( buf );
                        if ( buf.readBoolean() )
                        {
                            item.displayName = DefinedPacket.readString( buf );
                        }
                        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19 )
                        {
                            item.publicKey = readPublicKey( buf );
                        }
                        break;
                    case UPDATE_GAMEMODE:
                        item.gamemode = DefinedPacket.readVarInt( buf );
                        break;
                    case UPDATE_LATENCY:
                        item.ping = DefinedPacket.readVarInt( buf );
                        break;
                    case UPDATE_DISPLAY_NAME:
                        if ( buf.readBoolean() )
                        {
                            item.displayName = DefinedPacket.readString( buf );
                        }
                }
            }
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_8 )
        {
            Item item = items[0]; // Only one at a time
            writeString( item.displayName, buf ); // TODO: Server unique only!
            buf.writeBoolean( action != Action.REMOVE_PLAYER );
            buf.writeShort( item.ping );
        } else
        {
            DefinedPacket.writeVarInt( action.ordinal(), buf );
            DefinedPacket.writeVarInt( items.length, buf );
            for ( Item item : items )
            {
                DefinedPacket.writeUUID( item.uuid, buf );
                switch ( action )
                {
                    case ADD_PLAYER:
                        DefinedPacket.writeString( item.username, buf );
                        DefinedPacket.writeProperties( item.properties, buf );
                        DefinedPacket.writeVarInt( item.gamemode, buf );
                        DefinedPacket.writeVarInt( item.ping, buf );
                        buf.writeBoolean( item.displayName != null );
                        if ( item.displayName != null )
                        {
                            DefinedPacket.writeString( item.displayName, buf );
                        }
                        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19 )
                        {
                            writePublicKey( item.publicKey, buf );
                        }
                        break;
                    case UPDATE_GAMEMODE:
                        DefinedPacket.writeVarInt( item.gamemode, buf );
                        break;
                    case UPDATE_LATENCY:
                        DefinedPacket.writeVarInt( item.ping, buf );
                        break;
                    case UPDATE_DISPLAY_NAME:
                        buf.writeBoolean( item.displayName != null );
                        if ( item.displayName != null )
                        {
                            DefinedPacket.writeString( item.displayName, buf );
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

    public static enum Action
    {

        ADD_PLAYER,
        UPDATE_GAMEMODE,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME,
        REMOVE_PLAYER;
    }

    @Data
    public static class Item
    {

        // ALL
        private UUID uuid;

        // ADD_PLAYER
        private String username;
        private Property[] properties;
        private PlayerPublicKey publicKey;

        // ADD_PLAYER & UPDATE_GAMEMODE
        private int gamemode;

        // ADD_PLAYER & UPDATE_LATENCY
        private int ping;

        // ADD_PLAYER & UPDATE_DISPLAY_NAME
        private String displayName;

    }
}
