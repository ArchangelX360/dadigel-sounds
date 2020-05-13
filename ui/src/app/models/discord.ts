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
