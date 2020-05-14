export interface Channel {
  id: string;
  name: string;
}

export interface Guild {
  id: string;
  name: string;
}

export interface Connection {
  guild: Guild;
  channel: Channel;
}

export interface IsConnectedResponse {
  isConnected: boolean;
}

export interface BotStatus {
  state: BotState,
  joinedChannel: Channel | null,
  playingTrack: TrackInfo | null
}

export enum BotState {
  OFFLINE,
  LOGGED_IN,
  JOINED_IDLE,
  PLAYING
}

export interface TrackInfo {
  id: string;
  title: string;
}
