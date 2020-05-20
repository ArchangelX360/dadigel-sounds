export interface Channel {
  id: string;
  name: string;
}

export interface Guild {
  id: string;
  name: string;
}

export interface Connection {
  guildId: string;
  channelId: string;
}

export interface BotStatus {
  joinedChannel: Channel | null;
  playingTrack: TrackInfo | null;
}

export interface TrackInfo {
  id: string;
  title: string;
}
