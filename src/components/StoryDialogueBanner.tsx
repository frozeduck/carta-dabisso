import React from 'react';
import { DialogueMessage } from '../types/game';
import { ArrowRight, Scroll } from 'lucide-react';
import { AudioHelper } from '../audio/AudioHelper';

interface StoryDialogueBannerProps {
  dialogue: DialogueMessage;
  onDismiss: () => void;
}

export const StoryDialogueBanner: React.FC<StoryDialogueBannerProps> = ({
  dialogue,
  onDismiss,
}) => {
  const handleDismiss = () => {
    AudioHelper.playClick();
    onDismiss();
  };

  return (
    <div className="fixed inset-x-4 top-16 sm:top-20 z-40 flex justify-center select-none pointer-events-auto">
      <div className="parchment-window w-full max-w-2xl rounded-2xl p-4 flex items-start gap-4 shadow-2xl animate-in fade-in slide-in-from-top-4 duration-200 border-2 border-[#8c6239]">
        <div className="w-14 h-14 rounded-xl border-2 border-[#854d0e] p-0.5 bg-[#ebd9b4] flex-shrink-0 overflow-hidden shadow">
          <img
            src={dialogue.portrait}
            alt={dialogue.speaker}
            className="w-full h-full object-cover rounded-lg"
          />
        </div>

        <div className="flex-1">
          <div className="flex items-center justify-between mb-1">
            <h4 className="font-cinzel font-bold text-sm text-[#2b1706] flex items-center gap-1.5">
              <Scroll className="w-3.5 h-3.5 text-[#854d0e]" />
              {dialogue.speaker}
            </h4>
            <span className="text-[10px] text-[#854d0e] font-cinzel font-bold uppercase tracking-wider bg-[#eeddbb] px-2 py-0.5 rounded border border-[#bfa57b]">
              Dispaccio Nautico
            </span>
          </div>

          <p className="text-xs sm:text-sm text-[#4a2e12] leading-relaxed italic mb-2 font-serif">
            "{dialogue.text}"
          </p>

          <div className="flex justify-end">
            <button
              onClick={handleDismiss}
              className="px-3.5 py-1 rounded-lg parchment-button-gold font-cinzel font-bold text-xs flex items-center gap-1.5 shadow"
            >
              <span>Procedi</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
