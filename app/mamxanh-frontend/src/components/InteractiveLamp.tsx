import { useState } from 'react';

interface InteractiveLampProps {
  isOn: boolean;
  onToggle: () => void;
}

function playClickSound() {
  try {
    const AudioCtx = window.AudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(800, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(130, ctx.currentTime + 0.045);
    gain.gain.setValueAtTime(0.18, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.045);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.045);
  } catch {
    // Audio restrictions in background or before user interaction safely ignored
  }
}

export function InteractiveLamp({ isOn, onToggle }: InteractiveLampProps) {
  const [isPulling, setIsPulling] = useState(false);

  const handlePull = () => {
    setIsPulling(true);
    playClickSound();
    onToggle();
    setTimeout(() => {
      setIsPulling(false);
    }, 240);
  };

  return (
    <div data-testid="interactive-lamp" className="relative flex flex-col items-center justify-center select-none">
      {/* Quầng sáng vàng ấm tự nhiên quanh chao đèn (không bị viền hộp giới hạn) */}
      <div
        className={`pointer-events-none absolute left-1/2 top-14 -translate-x-1/2 -translate-y-1/2 h-[560px] w-[560px] rounded-full transition-opacity duration-700 ease-in-out ${
          isOn ? 'opacity-100' : 'opacity-0'
        }`}
        style={{
          background:
            'radial-gradient(circle, rgba(254, 240, 138, 0.55) 0%, rgba(250, 204, 21, 0.26) 28%, rgba(245, 158, 11, 0.07) 50%, rgba(245, 158, 11, 0) 70%)',
        }}
      />

      {/* Cấu trúc cây đèn tối giản (Lampshade + Stem + Cord + Base) */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Chao đèn hình vòm nấm (Mushroom Dome Lampshade) */}
        <div
          onClick={handlePull}
          className="group relative cursor-pointer transition-transform duration-300 hover:scale-[1.02]"
          title="Bật / tắt đèn"
          aria-label="Bật / tắt đèn"
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') handlePull();
          }}
        >
          {/* Vòm chao đèn */}
          <div
            className={`relative h-28 w-60 rounded-t-[130px] rounded-b-[10px] transition-all duration-500 ${
              isOn
                ? 'bg-gradient-to-b from-white via-amber-50 to-amber-100 shadow-[0_0_50px_15px_rgba(254,240,138,0.85),0_0_100px_35px_rgba(245,158,11,0.45),inset_0_-8px_18px_rgba(250,204,21,0.5)]'
                : 'bg-gradient-to-b from-[#2a2d35] to-[#1c1e24] shadow-[inset_0_2px_4px_rgba(255,255,255,0.06),0_8px_20px_rgba(0,0,0,0.6)]'
            }`}
          />

          {/* Dải sáng ấm đáy chao đèn */}
          <div
            className={`mx-auto h-2.5 w-52 rounded-full transition-all duration-500 ${
              isOn
                ? 'bg-gradient-to-r from-amber-300 via-yellow-100 to-amber-300 shadow-[0_4px_25px_rgba(253,224,71,1)]'
                : 'bg-neutral-800'
            }`}
          />
        </div>

        {/* Khu vực thân đèn & Dây giật */}
        <div className="relative flex items-start">
          {/* Cột thân đèn (Lamp pole) */}
          <div
            className={`h-48 w-4 rounded-full transition-colors duration-500 ${
              isOn
                ? 'bg-gradient-to-r from-[#ece7db] via-[#f7f4ea] to-[#ded6c4] shadow-[0_0_15px_rgba(253,224,71,0.35)]'
                : 'bg-gradient-to-r from-[#2a2c33] via-[#393c45] to-[#202227]'
            }`}
          />

          {/* Dây giật công tắc (Pull Cord & Amber Bead) */}
          <div
            onClick={(e) => {
              e.stopPropagation();
              handlePull();
            }}
            className="group absolute left-9 top-0 flex cursor-pointer flex-col items-center"
            title="Kéo dây để bật / tắt đèn"
            aria-label="Kéo dây công tắc đèn"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.stopPropagation();
                handlePull();
              }
            }}
            style={{
              transform: isPulling ? 'translateY(16px)' : 'translateY(0)',
              transition: isPulling
                ? 'transform 0.08s ease-in'
                : 'transform 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.35)',
            }}
          >
            {/* Sợi dây xích / dây kéo */}
            <div
              className={`w-[2.5px] h-20 transition-colors duration-500 ${
                isOn ? 'bg-amber-400/90 shadow-[0_0_6px_rgba(251,191,36,0.6)]' : 'bg-neutral-600'
              }`}
            />

            {/* Hạt tròn công tắc ở đầu dây */}
            <div
              className={`relative -mt-0.5 flex h-4 w-4 items-center justify-center rounded-full transition-all duration-300 group-hover:scale-125 ${
                isOn
                  ? 'bg-gradient-to-br from-amber-300 via-amber-500 to-amber-700 shadow-[0_0_14px_rgba(251,191,36,0.95)] ring-2 ring-amber-300/40'
                  : 'bg-gradient-to-br from-amber-400 to-amber-700 ring-2 ring-amber-400/50 shadow-[0_0_10px_rgba(251,191,36,0.5)] animate-pulse'
              }`}
            >
              <span className="h-1 w-1 rounded-full bg-white/90" />
            </div>
          </div>
        </div>

        {/* Đế đèn (Lamp Base) */}
        <div
          className={`h-3.5 w-36 rounded-full transition-all duration-500 ${
            isOn
              ? 'bg-gradient-to-r from-[#ece7db] via-[#f7f4ea] to-[#ded6c4] shadow-[0_4px_20px_rgba(251,191,36,0.45)]'
              : 'bg-gradient-to-r from-[#202227] via-[#33363f] to-[#202227] shadow-[0_4px_12px_rgba(0,0,0,0.8)]'
          }`}
        />
      </div>
    </div>
  );
}
