import {Injectable} from '@angular/core';
import {Sound} from '../models/sound';
import {DiscordService} from './discord.service';
import {from, Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Channel, Guild} from '../models/discord';

@Injectable()
export class SoundManagerService {
  sounds: Observable<Array<Sound>>;
  soundRoot = 'assets/sounds/';
  currentSong: HTMLAudioElement;
  mappings = [
    {fileName: 'abuse.mp3', tileName: 'Abusé!'},
    {
      fileName: 'ahnonmecjaccepteplustousca.mp3',
      tileName: 'Ah non mec, j\'accepte plus tous ça!',
    },
    {fileName: 'alleraplus.mp3', tileName: 'Aller à plus!'},
    {fileName: 'bataard.mp3', tileName: 'Bâtard!'},
    {fileName: 'bordeldemerde.mp3', tileName: 'Bordel de merde!'},
    {fileName: 'bravoleveau.mp3', tileName: 'Bravo le veau!'},
    {fileName: 'breath.mp3', tileName: 'Breath'},
    {fileName: 'cestlafolie.mp3', tileName: 'C\'est la folie!'},
    {
      fileName: 'cestmoibonjourvousallezbien.mp3',
      tileName: 'C\'est moi, bonjour, vous allez-bien?',
    },
    {fileName: 'cestpaslafolie.mp3', tileName: 'C\'est pas la folie!'},
    {
      fileName: 'cesttellementeasyquoi.mp3',
      tileName: 'C\'est tellement easy quoi',
    },
    {fileName: 'connard.mp3', tileName: 'Connard!'},
    {fileName: 'connarddemerde.mp3', tileName: 'Connard de merde!'},
    {fileName: 'dequoi.mp3', tileName: 'De quoi?'},
    {fileName: 'desdoigtsdanslecul.mp3', tileName: 'Des doigts dans le CUL!'},
    {fileName: 'ellechauffetropla.mp3', tileName: 'Elle chauffe trop là!'},
    {fileName: 'etouaimec.mp3', tileName: 'Et ouai mec!'},
    {
      fileName: 'etouiouiouiiijegagne.mp3',
      tileName: 'Et oui oui ouiii je gagne!',
    },
    {fileName: 'Euuhmetuepas.mp3', tileName: 'Euuh, me tue pas!'},
    {fileName: 'fdp.mp3', tileName: 'Fils de pute!'},
    {fileName: 'filsdechienva.mp3', tileName: 'Fils de chien va!'},
    {fileName: 'fredonner.mp3', tileName: 'Fredonner'},
    {fileName: 'fuckyou.mp3', tileName: 'Fuck you'},
    {fileName: 'gogogo.mp3', tileName: 'Go Go Go'},
    {fileName: 'ilperdpasdeviie.mp3', tileName: 'Il perd pas de vie!'},
    {
      fileName: 'jaifaisdelamerde.mp3',
      tileName: 'J\'ai fais de la merde! (mélodique)',
    },
    {
      fileName: 'jaifaisdelamerde2.mp3',
      tileName: 'J\'ai fais de la merde! (lutin)',
    },
    {fileName: 'jefaisdelamerde.mp3', tileName: 'Je fais de la merde!'},
    {fileName: 'jemecassewesh.mp3', tileName: 'Je me casse wesh!'},
    {fileName: 'jesucksduponeyquoi.mp3', tileName: 'Je sucks du poney quoi!'},
    {
      fileName: 'jesuckstellementunmaxquoi.mp3',
      tileName: 'Je sucks tellement un max quoi!',
    },
    {fileName: 'jesuisdesolemec.mp3', tileName: 'Je suis désolé mec!'},
    {fileName: 'jesuismort.mp3', tileName: 'Mort, je suis mort (melodique)'},
    {
      fileName: 'jesuistellementdesole.mp3',
      tileName: 'Je suis tellement désolé !',
    },
    {
      fileName: 'jesuistellementdesolepourtoiquoi.mp3',
      tileName: 'Je suis tellement désolé pour toi quoi',
    },
    {fileName: 'jesuisterrifiequoi.mp3', tileName: 'Je suis terrifié quoi'},
    {
      fileName: 'jetabandonnecommeunemerde.mp3',
      tileName: 'Je t\'abandonne comme une merde',
    },
    {fileName: 'jfaisdelamerde.mp3', tileName: 'De la merde'},
    {fileName: 'jfaisdelamerde2.mp3', tileName: 'J\'fais de la merde'},
    {
      fileName: 'jsuismortjsuismort.mp3',
      tileName: 'Je suis mort, je suis mort !',
    },
    {fileName: 'maaabienjouemec.mp3', tileName: 'Maaah bien joué mec'},
    {fileName: 'mec.mp3', tileName: 'Mec'},
    {fileName: 'merci.mp3', tileName: 'Merci'},
    {fileName: 'mescouilles.mp3', tileName: 'Mes couilles, mes couilles'},
    {
      fileName: 'mettonsnousaportee.mp3',
      tileName: 'Mettons nous à portée (lutin)',
    },
    {
      fileName: 'mettonsnousaportee2.mp3',
      tileName: 'Mettons nous à portée 2 (lutin)',
    },
    {fileName: 'NICE.mp3', tileName: 'Nice'},
    {fileName: 'niquetamere.mp3', tileName: 'Nique ta mère'},
    {
      fileName: 'niquetameresteuplait.mp3',
      tileName: 'Nique ta mère steu plait',
    },
    {fileName: 'niveaugagneee.mp3', tileName: 'Niveau gagnééé'},
    {fileName: 'non.mp3', tileName: 'Non'},
    {
      fileName: 'nonmecjaiarretejaiarrete.mp3',
      tileName: 'Non mec j\'ai arrêté, j\'ai arrêté',
    },
    {fileName: 'oh.mp3', tileName: 'Oh !'},
    {
      fileName: 'ohilperdtellementpasdeviiiieperdpasdevie.mp3',
      tileName: 'Oh il perd tellement pas de viiie, perd pas de vie',
    },
    {fileName: 'ohjemesuisfaisrape.mp3', tileName: 'Oh je me suis fait rape'},
    {fileName: 'ohlavaaache.mp3', tileName: 'Oh la vaaache'},
    {fileName: 'ohlavachequoi.mp3', tileName: 'Oh la vache quoi'},
    {fileName: 'ohmerde.mp3', tileName: 'Oh merde !'},
    {fileName: 'ohyeahyeahyeahyeah.mp3', tileName: 'Oh yeah yeah yeah yeah'},
    {fileName: 'onestplutotcontent.mp3', tileName: 'On est plutôt content'},
    {
      fileName: 'onesttellementtristela.mp3',
      tileName: 'On est tellement triste là',
    },
    {fileName: 'ontedeteste.mp3', tileName: 'On te déteste'},
    {fileName: 'onvatetuer.mp3', tileName: 'On va te tuer !'},
    {fileName: 'ooohhh.mp3', tileName: 'Ooohhh'},
    {fileName: 'ooohmerde.mp3', tileName: 'Oooh meeerde'},
    {fileName: 'Ooohoh.mp3', tileName: 'Oooh oh ! (peur)'},
    {fileName: 'ooooui.mp3', tileName: 'Ooooui (jouissance)'},
    {fileName: 'ouh.mp3', tileName: 'Ouh (extase)'},
    {fileName: 'pacebook.mp3', tileName: 'Pacebook'},
    {fileName: 'parpitie.mp3', tileName: 'Par pitié'},
    {fileName: 'pitie.mp3', tileName: 'Pitié'},
    {fileName: 'pitie2.mp3', tileName: 'Pitié 2'},
    {fileName: 'pleurs.mp3', tileName: 'Pleurs'},
    {
      fileName: 'prendtapotionfilsdepute.mp3',
      tileName: 'Prend ta potion fils de pute (mélodique)',
    },
    {
      fileName: 'ptainjemesuisfaiseclate.mp3',
      tileName: 'Ptain je me suis fait éclater',
    },
    {
      fileName: 'ptaintumangestellementdesbites.mp3',
      tileName: 'Ptain tu manges tellement des bites',
    },
    {
      fileName: 'puckyoudanslesfesses.mp3',
      tileName: 'Puck you dans les fesses',
    },
    {
      fileName: 'questcequetuveuxfaire.mp3',
      tileName: 'Qu\'est ce que tu veux faire',
    },
    {fileName: 'rire.mp3', tileName: 'Rire'},
    {fileName: 'rire2.mp3', tileName: 'Rire 2'},
    {fileName: 'rire3.mp3', tileName: 'Rire 3'},
    {fileName: 'rire4.mp3', tileName: 'Rire 4'},
    {fileName: 'rire5.mp3', tileName: 'Rire 5'},
    {fileName: 'rire6.mp3', tileName: 'Rire 6'},
    {fileName: 'rireohmec.mp3', tileName: 'Rire , oh mec'},
    {fileName: 'serieux.mp3', tileName: 'Sérieux ?!'},
    {
      fileName: 'sijedeconnectecestnormalejauraiexplose.mp3',
      tileName: 'Si je déconnecte c\'est normal, j\'aurai explosé',
    },
    {
      fileName: 'tainjesucksacejevideo.mp3',
      tileName: '\'Tain je sucks à ce jeu vidéo',
    },
    {fileName: 'tainjesuiscon.mp3', tileName: '\'Tain je suis con'},
    {
      fileName: 'taintaintesundingue.mp3',
      tileName: '\'Tain \'tain t\'es un dingue',
    },
    {fileName: 'terrifiemongars.mp3', tileName: 'Terrifié mon gars'},
    {fileName: 'tesoufouquoi.mp3', tileName: 'T\'es ouf ou quoi'},
    {
      fileName: 'tespasobligedevenir.mp3',
      tileName: 'T\'es pas obligé de venir, t\'es pas obligé de venir',
    },
    {fileName: 'testellementtriste.mp3', tileName: 'T\'es tellement triste'},
    {fileName: 'tupeuxrienfairequoi.mp3', tileName: 'Tu peux rien faire quoi'},
    {fileName: 'tuvasfairequoi.mp3', tileName: 'Tu vas faire quoi !'},
    {fileName: 'tuveuxdesboulettes.mp3', tileName: 'Tu veux des boulettes ?!'},
    {
      fileName: 'tuveuxdesboulettesbatard.mp3',
      tileName: 'Tu veux des boulettes bâtard ?!',
    },
    {
      fileName: 'vousvoulezvoirlefeudartifice.mp3',
      tileName: 'Vous voulez voir le feu d\'artifice ?',
    },
    {fileName: 'WHAAAT.mp3', tileName: 'WHAAAT !'},
    {fileName: 'what.mp3', tileName: 'What'},
    {fileName: 'whatdoyouwanttodo.mp3', tileName: 'What do you want to do ?'},
    {fileName: 'whatwhatwhat.mp3', tileName: 'What what what'},
    {fileName: 'yeah.mp3', tileName: 'Yeeeah'},
  ];

  constructor(private discordService: DiscordService) {
    this.sounds = this.discordService.getSounds().pipe(
      map(sounds =>
        sounds.reduce((acc, s) => {
          const sound = this.mappings.find(m => m.fileName === s.toLowerCase());
          return sound ? [...acc, sound] : acc;
        }, []),
      ),
    );
  }

  playSound(
    filename: string,
    guild?: Guild,
    channel?: Channel,
  ): Observable<string> {
    if (!this.mappings.find(m => m.fileName === filename)) {
      throw new Error('sound not found');
    }

    if (guild?.id && channel?.id) {
      return this.discordService.playSoundInChannel(
        guild.id,
        channel.id,
        filename,
      );
    } else {
      if (this.currentSong) {
        this.currentSong.pause();
      }
      // eslint-disable-next-line no-undef
      this.currentSong = new Audio(this.soundRoot + filename);
      this.currentSong.load();
      return from(this.currentSong.play()).pipe(
        map(() => {
          return 'successfully played locally';
        }),
      );
    }
  }

  getSounds(): Observable<Array<Sound>> {
    return this.sounds;
  }
}
