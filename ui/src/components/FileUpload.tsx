'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { FiUpload } from 'react-icons/fi';

interface FileUploadProps {
  onFileUpload: (file: File) => void;
  isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      setErrorMessage('');

      if (acceptedFiles.length > 0) {
        const file = acceptedFiles[0];

        // ❗ Check file size
        if (file.size > MAX_FILE_SIZE) {
          setErrorMessage('❌ File too large! Max size is 10MB.');
          return;
        }

        onFileUpload(file);
      }
    },
    [onFileUpload]
  );

  const { getRootProps, getInputProps } = useDropzone({
    onDrop,
    multiple: false,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
    onDropAccepted: () => setDragActive(false),
    onDropRejected: () => setDragActive(false),
  });

  return (
    <div>
      <div
        {...getRootProps()}
        className={`
          w-full p-8 border-2 border-dashed rounded-lg text-center cursor-pointer transition-all
          ${dragActive
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 hover:border-blue-400 hover:bg-gray-50'}
          ${isUploading ? 'opacity-50 pointer-events-none' : ''}
        `}
      >
        <input {...getInputProps()} />
        <div className="flex flex-col items-center justify-center space-y-3">
          <div className="p-3 bg-blue-100 rounded-full">
            <FiUpload className="w-6 h-6 text-blue-500" />
          </div>
          <p className="text-lg font-medium">
            Drag & drop a file here, or click to select
          </p>
          <p className="text-sm text-gray-500">
            Share any file with your peers securely
          </p>
        </div>
      </div>

      {/* ✅ Error message under dropzone */}
      {errorMessage && (
        <p className="mt-4 text-sm text-red-600 text-center">{errorMessage}</p>
      )}
    </div>
  );
}
